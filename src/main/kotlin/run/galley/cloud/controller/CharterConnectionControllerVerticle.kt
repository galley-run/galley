package run.galley.cloud.controller

import generated.jooq.enums.OAuthConnectionStatus
import generated.jooq.enums.OAuthConnectionType
import generated.jooq.enums.OAuthCredentialKind
import generated.jooq.enums.OAuthGrantPermission
import generated.jooq.enums.OAuthGrantPrincipalType
import generated.jooq.enums.OAuthProvider
import generated.jooq.tables.pojos.OAuthConnections
import generated.jooq.tables.references.OAUTH_CONNECTIONS
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.oauth2.OAuth2Auth
import io.vertx.ext.auth.oauth2.OAuth2AuthorizationURL
import io.vertx.ext.auth.oauth2.OAuth2Options
import io.vertx.ext.auth.oauth2.Oauth2Credentials
import io.vertx.ext.auth.oauth2.providers.GithubAuth
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.api.Pagination
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.eventbus.filters
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.coroutineEventBus
import nl.clicqo.ext.toUUID
import nl.clicqo.web.HttpStatus
import run.galley.cloud.ApiStatus
import run.galley.cloud.data.OAuthConnectionDataVerticle
import run.galley.cloud.util.Encryption
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CharterConnectionControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val LIST = "charter.connection.query.list"
    const val CREATE = "charter.connection.cmd.create"
    const val APPROVE = "charter.connection.cmd.approve"
  }

  private lateinit var webClient: WebClient

  override suspend fun start() {
    super.start()

    webClient = WebClient.create(vertx)

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(APPROVE, handler = ::approve)
    }
  }

  private fun createOAuth2Provider(
    provider: OAuthProvider,
    clientId: String?,
    clientSecret: String?,
  ): OAuth2Auth? {
    if (clientId == null || clientSecret == null) return null

    return when (provider) {
      OAuthProvider.github -> {
        GithubAuth.create(
          vertx,
          clientId,
          clientSecret,
        )
      }

      OAuthProvider.gitlab -> {
        OAuth2Auth.create(
          vertx,
          OAuth2Options()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setSite("https://gitlab.com")
            .setTokenPath("/oauth/token")
            .setAuthorizationPath("/oauth/authorize")
            .setUserInfoPath("/api/v4/user"),
        )
      }

      OAuthProvider.bitbucket -> {
        OAuth2Auth.create(
          vertx,
          OAuth2Options()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setSite("https://bitbucket.org")
            .setTokenPath("/site/oauth2/access_token")
            .setAuthorizationPath("/site/oauth2/authorize")
            .setUserInfoPath("/2.0/user"),
        )
      }

      else -> {
        null
      }
    }
  }

  private suspend fun fetchAccountInfo(
    provider: OAuthProvider,
    accessToken: String,
  ): JsonObject? =
    try {
      when (provider) {
        OAuthProvider.github -> {
          webClient
            .get(443, "api.github.com", "/user")
            .ssl(true)
            .bearerTokenAuthentication(accessToken)
            .send()
            .coAwait()
            .bodyAsJsonObject()
        }

        OAuthProvider.gitlab -> {
          webClient
            .get(443, "gitlab.com", "/api/v4/user")
            .ssl(true)
            .bearerTokenAuthentication(accessToken)
            .send()
            .coAwait()
            .bodyAsJsonObject()
        }

        OAuthProvider.bitbucket -> {
          webClient
            .get(443, "api.bitbucket.org", "/2.0/user")
            .ssl(true)
            .bearerTokenAuthentication(accessToken)
            .send()
            .coAwait()
            .bodyAsJsonObject()
        }

        else -> {
          null
        }
      }
    } catch (e: Exception) {
      null
    }

  private suspend fun list(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId

    // Get query parameters for filtering
    val providerParam = apiRequest.query?.get("provider")?.string
    val typeParam = apiRequest.query?.get("type")?.string
    val statusParam = apiRequest.query?.get("status")?.string

    val dataRequest =
      EventBusQueryDataRequest(
        filters =
          filters {
            OAUTH_CONNECTIONS.CHARTER_ID eq charterId
            OAUTH_CONNECTIONS.VESSEL_ID eq vesselId

            if (providerParam != null) {
              OAUTH_CONNECTIONS.PROVIDER eq OAuthProvider.valueOf(providerParam)
            }
            if (typeParam != null) {
              OAUTH_CONNECTIONS.TYPE eq OAuthConnectionType.valueOf(typeParam)
            }
            if (statusParam != null) {
              OAUTH_CONNECTIONS.STATUS eq OAuthConnectionStatus.valueOf(statusParam)
            }
          },
        pagination = Pagination(offset = 0, limit = 100),
      )

    val connections =
      vertx
        .eventBus()
        .request<EventBusDataResponse<OAuthConnections>>(OAuthConnectionDataVerticle.LIST, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toMany() ?: emptyList()

    // For each connection, get the user's permissions and account_login from credentials
    val connectionsWithPermissions =
      connections.map { connection ->
        val permissions = getUserPermissionsForConnection(connection.id!!, userId)
        val accountLogin = getAccountLoginForConnection(connection.id!!)

        JsonObject()
          .put("id", connection.id.toString())
          .put("vessel_id", connection.vesselId?.toString())
          .put("charter_id", connection.charterId?.toString())
          .put("type", connection.type.toString())
          .put("provider", connection.provider.toString())
          .put("status", connection.status.toString())
          .put("display_name", connection.displayName)
          .put("created_by_user_id", connection.createdByUserId.toString())
          .put("provider_account_id", connection.providerAccountId)
          .put("account_login", accountLogin)
          .put("last_validated_at", connection.lastValidatedAt?.toString())
          .put("created_at", connection.createdAt.toString())
          .put("user_permissions", JsonArray(permissions.map { it.toString() }))
      }

    val dataResponse = JsonArray(connectionsWithPermissions)
    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun create(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId
    val body = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)

    // Parse request body
    val type =
      body.getString("type")?.let { OAuthConnectionType.valueOf(it) }
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_TYPE_MISSING)
    val provider =
      body.getString("provider")?.let { OAuthProvider.valueOf(it) }
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_PROVIDER_MISSING)
    val displayName = body.getString("display_name")
    val requestedScopes = body.getJsonArray("scopes") ?: JsonArray()

    // Build connection payload
    val connectionPayload =
      JsonObject()
        .put("vessel_id", vesselId?.toString())
        .put("charter_id", charterId?.toString())
        .put("type", type.name)
        .put("provider", provider.name)
        .put("status", OAuthConnectionStatus.pending.name)
        .put("display_name", displayName)
        .put("created_by_user_id", userId.toString())
        .put("scopes", requestedScopes)

    // Store pending connection in database
    val dataRequest =
      EventBusCmdDataRequest(
        payload = connectionPayload,
        userId = userId,
      )

    val createdConnection =
      vertx
        .eventBus()
        .request<EventBusDataResponse<OAuthConnections>>(OAuthConnectionDataVerticle.CREATE, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CONNECTION_CREATE_FAILURE)

    // Generate OAuth authorization URL
    val oauth2Provider = getOAuth2Provider(provider)
    val redirectUri = "${
      config.getJsonObject(
        "cloud.galley.run",
      ).getString("protocol")
    }${config.getJsonObject("cloud.galley.run").getString("host")}/callback/oauth/${provider.literal}"
    val state = createdConnection.id.toString() // Use connection ID as state

    val authorizationUri =
      oauth2Provider.authorizeURL(
        OAuth2AuthorizationURL(
          JsonObject()
            .put("redirect_uri", redirectUri)
            .put("state", state)
            .put("scopes", requestedScopes),
        ),
      )

    // Return authorization URL to frontend
    val response =
      JsonObject()
        .put("id", createdConnection.id.toString())
        .put("type", "OAuthConnectionCreate")
        .put(
          "attributes",
          JsonObject()
            .put("authorization_url", authorizationUri)
            .put("status", "pending"),
        )

    message.reply(EventBusApiResponse(response, httpStatus = HttpStatus.Created))
  }

  private suspend fun getUserPermissionsForConnection(
    connectionId: UUID,
    userId: UUID,
  ): List<OAuthGrantPermission> {
    // TODO: Query grants for this user (direct user grants + role grants)
    // For now, return empty list
    return emptyList()
  }

  private suspend fun getAccountLoginForConnection(connectionId: UUID): String? {
    // TODO: Query credential for account_login
    return null
  }

  private fun getOAuth2Provider(provider: OAuthProvider): OAuth2Auth {
    val providerConfig = config.getJsonObject("oauth").getJsonObject(provider.name)
    val clientId = providerConfig.getString("client_id")
    val clientSecret = providerConfig.getString("client_secret")

    return when (provider) {
      OAuthProvider.github -> {
        GithubAuth.create(
          vertx,
          clientId,
          clientSecret,
        )
      }

      OAuthProvider.gitlab -> {
        val site = config.getString("oauth.gitlab.site") ?: "https://gitlab.com"
        OAuth2Auth.create(
          vertx,
          OAuth2Options()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setSite(site)
            .setAuthorizationPath("/oauth/authorize")
            .setTokenPath("/oauth/token")
            .setUserInfoPath("/api/v4/user"),
        )
      }

      OAuthProvider.bitbucket -> {
        OAuth2Auth.create(
          vertx,
          OAuth2Options()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setSite("https://bitbucket.org")
            .setAuthorizationPath("/site/oauth2/authorize")
            .setTokenPath("/site/oauth2/access_token")
            .setUserInfoPath("/2.0/user"),
        )
      }

      OAuthProvider.dockerhub -> {
        TODO()
      }

      OAuthProvider.ghcr -> {
        TODO()
      }
    }
  }

  private suspend fun approve(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val body = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)

    val provider =
      body.getString("provider")?.let { OAuthProvider.valueOf(it) }
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_PROVIDER_MISSING)
    val code =
      body.getString("code")
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CODE_MISSING)
    val state =
      body.getString("state")
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_STATE_MISSING)
    val installationId = body.getLong("installation_id")

    // Find the pending connection by state (connection ID)
    val connectionId = UUID.fromString(state)

    // Exchange code for access token
    val oauth2Provider = getOAuth2Provider(provider)
    val redirectUri = "${
      config.getJsonObject(
        "cloud.galley.run",
      ).getString("protocol")
    }${config.getJsonObject("cloud.galley.run").getString("host")}/callback/oauth/${provider.literal}"

    val credentials =
      Oauth2Credentials()
        .setFlow(io.vertx.ext.auth.oauth2.OAuth2FlowType.AUTH_CODE)
        .setCode(code)
        .setRedirectUri(redirectUri)

    val tokenResponse =
      oauth2Provider
        .authenticate(credentials)
        .coAwait()

    val accessToken = tokenResponse.principal().getString("access_token")
    val refreshToken = tokenResponse.principal().getString("refresh_token")
    val tokenType = tokenResponse.principal().getString("token_type") ?: "Bearer"
    val expiresIn = tokenResponse.principal().getLong("expires_in")

    // Fetch account information from provider
    val accountInfo =
      fetchAccountInfo(provider, accessToken)
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CONNECTION_APPROVAL_FAILURE)

    val providerAccountId = accountInfo.getValue("id").toString()
    val accountLogin =
      when (provider) {
        OAuthProvider.github -> accountInfo.getString("login")
        OAuthProvider.gitlab -> accountInfo.getString("username")
        OAuthProvider.bitbucket -> accountInfo.getString("username")
        else -> null
      }
    val accountType =
      when (provider) {
        OAuthProvider.github -> accountInfo.getString("type")
        else -> null
      }

    val now = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)

    // Update connection to active with provider account info
    val updatePayload =
      JsonObject()
        .put("status", OAuthConnectionStatus.active.name)
        .put("provider_account_id", providerAccountId)
        .put("last_validated_at", now.toString())

    val connection =
      vertx
        .eventBus()
        .request<EventBusDataResponse<OAuthConnections>>(
          OAuthConnectionDataVerticle.PATCH,
          EventBusCmdDataRequest(payload = updatePayload, identifier = connectionId, userId = userId),
        ).coAwait()
        .body()
        .payload
        ?.toOne() ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CONNECTION_NOT_FOUND)

    // 2. Delete old credentials if they exist (for connection replacement scenarios)
    vertx
      .eventBus()
      .request<EventBusDataResponse<OAuthConnections>>(
        OAuthConnectionDataVerticle.DELETE_CREDENTIALS,
        EventBusCmdDataRequest(identifier = connectionId, userId = userId),
      ).coAwait()

    // 3. Create new credential
    // Determine credential kind based on provider and installation_id
    val credentialKind =
      if (provider == OAuthProvider.github && installationId != null) {
        OAuthCredentialKind.github_app_installation
      } else {
        OAuthCredentialKind.oauth_token
      }

    // Get encryption key from config
    val encryptionKey =
      config.getJsonObject("jwt")?.getString("pepper")
        ?: throw ApiStatusReplyException(ApiStatus.JWT_PEPPER_MISSING)

    // Get app_id from provider config if available (for GitHub Apps)
    val appId =
      if (provider == OAuthProvider.github) {
        config
          .getJsonObject("oauth")
          ?.getJsonObject(provider.name)
          ?.getLong("app_id")
      } else {
        null
      }

    val credentialPayload =
      JsonObject()
        .put("connection_id", connectionId.toString())
        .put("credential_kind", credentialKind.name)
        .put("access_token_encrypted", Encryption.encrypt(accessToken, encryptionKey))
        .put("refresh_token_encrypted", Encryption.encrypt(refreshToken, encryptionKey))
        .put("token_type", tokenType)
        .put("expires_in", expiresIn)
        .put("installation_id", installationId)
        .put("app_id", appId)
        .put("account_login", accountLogin)
        .put("account_type", accountType)

    vertx
      .eventBus()
      .request<EventBusDataResponse<OAuthConnections>>(
        OAuthConnectionDataVerticle.CREATE_CREDENTIAL,
        EventBusCmdDataRequest(payload = credentialPayload, userId = userId),
      ).coAwait()

    // 4. Create grants (or skip if they already exist due to unique constraint)
    val grants =
      listOf(
        JsonObject()
          .put("connection_id", connectionId.toString())
          .put("principal_type", OAuthGrantPrincipalType.user.name)
          .put("principal_id", userId.toString())
          .put("permission", OAuthGrantPermission.manage.name),
        JsonObject()
          .put("connection_id", connectionId.toString())
          .put("principal_type", OAuthGrantPrincipalType.user.name)
          .put("principal_id", userId.toString())
          .put("permission", OAuthGrantPermission.revoke.name),
        JsonObject()
          .put("connection_id", connectionId.toString())
          .put("principal_type", OAuthGrantPrincipalType.role.name)
          .put("principal_id", "member")
          .put("permission", OAuthGrantPermission.use.name),
      )

    for (grant in grants) {
      vertx
        .eventBus()
        .request<EventBusDataResponse<OAuthConnections>>(
          OAuthConnectionDataVerticle.CREATE_GRANT,
          EventBusCmdDataRequest(payload = grant, userId = userId),
        ).coAwait()
    }

    message.reply(EventBusApiResponse(connection.toJsonAPIResourceObject()))
  }
}
