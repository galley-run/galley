package nl.clicqo.api

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.KeyStoreOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.router.RouterBuilder
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.openapi.contract.OpenAPIContract

open class OpenAPIBridgeRouter(open val config: JsonObject) {
  protected lateinit var openAPIRouterBuilder: RouterBuilder
  protected lateinit var authProvider: JWTAuth

  open suspend fun buildRouter(vertx: Vertx): RouterBuilder {
    val openAPIFile = config.getJsonObject("api").getString("openapiFile", "openapi.yaml")
    val contract = OpenAPIContract.from(vertx, openAPIFile).coAwait()
    this.openAPIRouterBuilder = RouterBuilder.create(vertx, contract)
    this.authProvider = JWTAuth.create(vertx, authConfig)

    return openAPIRouterBuilder
  }

  open fun createRouter(): Router = openAPIRouterBuilder.createRouter()

  protected val authConfig: JWTAuthOptions = JWTAuthOptions()
    .setKeyStore(
      KeyStoreOptions()
        .setType(config.getJsonObject("jwt", JsonObject()).getString("type", "jceks"))
        .setPath(config.getJsonObject("jwt", JsonObject()).getString("keystore", "keystore.jceks"))
        .setPassword(config.getJsonObject("jwt", JsonObject()).getString("secret", ""))
    )

  suspend fun catchAll(routingContext: RoutingContext, fn: suspend () -> Unit) {
    try {
      fn()
    } catch (e: Throwable) {
      routingContext.fail(e)
    }
  }

  /**
   * Generates an address string based on the provided operation ID.
   *
   * The logic determines the domain and verb components of the address
   * according to predefined rules for operation IDs. The result is a formatted string
   * that categorizes the operation as either a query or a command, depending on its prefix.
   *
   * Operation IDs starting with:
   * - "list" or "get" are categorized as queries, and their domain is suffixed with `.query.<verb>`.
   * - "create", "update", or "delete" are categorized as commands, and their domain is suffixed with `.cmd.<verb>`.
   * - Other prefixes result in the address being prefixed with "api." followed by the operation ID.
   *
   * Uses the `toDomain` function to derive the domain from the operation ID and the `toVerb` function to identify the verb.
   *
   * @param operationId The operation identifier used to generate the address string.
   * @return A string representing the constructed address.
   */
  protected val createAddress: (operationId: String) -> String = { id ->
    when {
      id.startsWith("list") -> id.toDomain() + ".query." + id.toVerb()
      id.startsWith("get") -> id.toDomain() + ".query." + id.toVerb()
      id.startsWith("create") -> id.toDomain() + ".cmd." + id.toVerb()
      id.startsWith("update") -> id.toDomain() + ".cmd." + id.toVerb()
      id.startsWith("delete") -> id.toDomain() + ".cmd." + id.toVerb()
      else -> "api.$id"
    }
  }

  /**
   * Transforms a given string, typically an operation ID, to its domain representation.
   *
   * The method removes common prefixes such as "list", "get", "create", "update", and "delete"
   * from the string. It then converts the first character of the remaining string to lowercase
   * if it is uppercase, extracts only the initial sequence of lowercase characters,
   * and defaults to "api" if the resulting string is blank.
   *
   * @receiver The string to be transformed into a domain representation.
   * @return The domain name derived from the original string.
   */
  private fun String.toDomain() =
    this
      .removePrefix("list")
      .removePrefix("get")
      .removePrefix("create")
      .removePrefix("update")
      .removePrefix("delete")
      .let { s ->
        s.replaceFirstChar { if (it.isUpperCase()) it.lowercase() else it.toString() }
      }
      .takeWhile { it.isLowerCase() }
      .let { word ->
        when {
          word.endsWith("ies") -> word.removeSuffix("ies") + "y"
          word.endsWith("ses") -> word.removeSuffix("es")
          word.endsWith("shes") -> word.removeSuffix("es")
          word.endsWith("ches") -> word.removeSuffix("es")
          word.endsWith("xes") -> word.removeSuffix("es")
          word.endsWith("s") && !word.endsWith("ss") -> word.removeSuffix("s")
          else -> word
        }
      }
      .ifBlank { "api" }

  /**
   * Determines the verb representation of the string based on its prefix.
   *
   * The method inspects the beginning of the string and returns a corresponding
   * action verb such as "list", "get", "create", "update", or "delete". If the
   * string does not match any of these prefixes, it defaults to "get".
   *
   * @receiver The string from which the verb is to be extracted.
   * @return A verb derived from the prefix of the string.
   */
  private fun String.toVerb() = when {
    startsWith("list") -> "list"
    startsWith("get") -> "get"
    startsWith("create") -> "create"
    startsWith("update") -> "update"
    startsWith("delete") -> "delete"
    else -> "get"
  }
}
