package run.galley.cloud.web

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.VertxException
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.audit.Marker
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.HttpException
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.ext.web.handler.impl.HTTPAuthorizationHandler
import io.vertx.ext.web.impl.RoutingContextInternal
import io.vertx.ext.web.internal.handler.ScopedAuthentication
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.UserRole
import java.util.Objects
import java.util.function.Function

class JWTAuthHandlerScp :
  HTTPAuthorizationHandler<JWTAuth?>,
  JWTAuthHandler,
  ScopedAuthentication<JWTAuthHandler?> {
  private val scopes: MutableList<String?>
  private var delimiter: String

  constructor(authProvider: JWTAuth?) : super(authProvider, Type.BEARER, null) {
    scopes = mutableListOf<String?>()
    this.delimiter = " "
  }

  constructor(authProvider: JWTAuth?, realm: String?) : super(authProvider, Type.BEARER, realm) {
    scopes = mutableListOf<String?>()
    this.delimiter = " "
  }

  private constructor(
    base: JWTAuthHandlerScp,
    scopes: MutableList<String?>,
    delimiter: String,
  ) : super(base.authProvider, Type.BEARER, base.realm) {
    Objects.requireNonNull<MutableList<String?>?>(scopes, "scopes cannot be null")
    this.scopes = scopes
    Objects.requireNonNull<String?>(delimiter, "delimiter cannot be null")
    this.delimiter = delimiter
  }

  override fun authenticate(context: RoutingContext): Future<User?>? {
    return parseAuthorization(context)
      .compose<User?>(
        Function { token: String? ->
          var segments = 0
          for (i in 0..<token!!.length) {
            val c = token.get(i)
            if (c == '.') {
              if (++segments == 3) {
                return@Function Future.failedFuture(HttpException(400, "Too many segments in token"))
              }
              continue
            }
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
              continue
            }
            // invalid character
            return@Function Future.failedFuture(HttpException(400, "Invalid character in token: " + c.code))
          }

          val credentials = TokenCredentials(token)
          val audit = (context as RoutingContextInternal).securityAudit()
          audit.credentials(credentials)
          authProvider!!
            .authenticate(TokenCredentials(token))
            .andThen { op: AsyncResult<User?>? -> audit.audit(Marker.AUTHENTICATION, op!!.succeeded()) }
            .recover { err: Throwable? -> Future.failedFuture(HttpException(401, err)) }
        },
      )
  }

  override fun withScope(scope: String?): JWTAuthHandler {
    Objects.requireNonNull<String?>(scope, "scope cannot be null")
    val updatedScopes: MutableList<String?> = ArrayList<String?>(this.scopes)
    updatedScopes.add(scope)
    return JWTAuthHandlerScp(this, updatedScopes, delimiter)
  }

  override fun withScopes(scopes: MutableList<String?>): JWTAuthHandler {
    Objects.requireNonNull(scopes, "scopes cannot be null")
    return JWTAuthHandlerScp(this, scopes, delimiter)
  }

  override fun scopeDelimiter(delimiter: String): JWTAuthHandler {
    Objects.requireNonNull(delimiter, "delimiter cannot be null")
    this.delimiter = delimiter
    return this
  }

  /**
   * The default behavior for post-authentication
   */
  override fun postAuthentication(ctx: RoutingContext) {
    val user = ctx.user()
    if (user == null) {
      // bad state
      ctx.fail(403, VertxException("no user in the context", true))
      return
    }
    // the user is authenticated, however the user may not have all the required scopes
    val scopes = getScopesOrSearchMetadata(this.scopes, ctx)

    if (scopes.isNotEmpty()) {
      val jwt = user.get<JsonObject?>("accessToken")
      if (jwt == null) {
        ctx.fail(403, VertxException("Invalid JWT: null", true))
        return
      }

      if (jwt.getValue("scp") == null) {
        ctx.fail(403, VertxException("Invalid JWT: scp claim is required", true))
        return
      }
      val target = jwt.getJsonObject("scp")

      if (target != null) {
        for (scope in scopes) {
          if (scope == UserRole.VESSEL_CAPTAIN.name && (
              ctx
                .pathParam("vesselId")
                .isNullOrBlank() ||
                target.getString("vessel:${ctx.pathParam("vesselId")}") != UserRole.VESSEL_CAPTAIN.name
            )
          ) {
            throw ApiStatus.CREW_NO_VESSEL_CAPTAIN
          }

          if (!ctx.pathParam("charterId").isNullOrBlank() &&
            target
              .getString("charter:${ctx.pathParam("charterId")}") != scope
          ) {
            throw ApiStatus.USER_ROLE_FORBIDDEN
          }
        }
      }
    }
    ctx.next()
  }
}
