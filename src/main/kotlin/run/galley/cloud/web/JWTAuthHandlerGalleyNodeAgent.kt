package run.galley.cloud.web

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.VertxException
import io.vertx.ext.auth.User
import io.vertx.ext.auth.audit.Marker
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.HttpException
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.ext.web.handler.impl.HTTPAuthorizationHandler
import io.vertx.ext.web.impl.RoutingContextInternal
import java.util.function.Function

class JWTAuthHandlerGalleyNodeAgent(
  base: JWTAuth,
) : HTTPAuthorizationHandler<JWTAuth?>(base, Type.BEARER, "galley-node-agent"),
  JWTAuthHandler {
  // Configuratie voor scoping
  private var scopeDelimiter: String = DEFAULT_SCOPE_DELIMITER
  private var requiredScopes: List<String> = emptyList()

  companion object {
    private const val DEFAULT_SCOPE_DELIMITER = " "
  }

  override fun authenticate(context: RoutingContext): Future<User?>? {
    return parseAuthorization(context)
      .compose(
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

    // Indien er scopes vereist zijn, controleer ze nu.
    if (requiredScopes.isNotEmpty()) {
      val principal = user.principal()
      val tokenScopes = extractTokenScopes(principal, scopeDelimiter)
      val missing = requiredScopes.filterNot { tokenScopes.contains(it) }
      if (missing.isNotEmpty()) {
        ctx.fail(HttpException(403, "Missing required scope(s): ${missing.joinToString(",")}"))
        return
      }
    }

    ctx.next()
  }

  // Fluent configuratie voor scope delimiter (niet-nullable, geeft this terug)
  override fun scopeDelimiter(delimiter: String?): JWTAuthHandler {
    this.scopeDelimiter = (delimiter ?: DEFAULT_SCOPE_DELIMITER).ifBlank { DEFAULT_SCOPE_DELIMITER }
    return this
  }

  // Voeg één scope toe (niet-nullable, geeft this terug)
  override fun withScope(scope: String?): JWTAuthHandler {
    val normalized = normalizeScope(scope)
    if (normalized != null) {
      this.requiredScopes = (this.requiredScopes + normalized).distinct()
    }
    return this
  }

  // Vervang/voeg meerdere scopes (filter leeg/null, geeft this terug)
  override fun withScopes(scopes: List<String?>?): JWTAuthHandler {
    val normalized = scopes.orEmpty().mapNotNull { normalizeScope(it) }
    if (normalized.isNotEmpty()) {
      this.requiredScopes = (this.requiredScopes + normalized).distinct()
    }
    return this
  }

  // Helpers

  private fun normalizeScope(scope: String?): String? =
    scope
      ?.trim()
      ?.takeIf { it.isNotEmpty() }

  private fun extractTokenScopes(
    principal: io.vertx.core.json.JsonObject,
    delimiter: String,
  ): Set<String> {
    // Probeer standaard JWT-veld "scope" of "scp" (string of array)
    val scopesFromScopeString =
      principal
        .getString("scope")
        ?.split(delimiter)
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        ?: emptySet()

    val scopesFromScp =
      when (val scp = principal.getValue("scp")) {
        is String ->
          scp
            .split(delimiter)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        is io.vertx.core.json.JsonArray ->
          scp.list
            .filterIsInstance<String>()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        is io.vertx.core.json.JsonObject -> {
          // Voor compatibiliteit als "scp" claims als object komen; neem keys als scopes
          scp.fieldNames().toSet()
        }

        else -> emptySet()
      }

    return scopesFromScopeString + scopesFromScp
  }
}
