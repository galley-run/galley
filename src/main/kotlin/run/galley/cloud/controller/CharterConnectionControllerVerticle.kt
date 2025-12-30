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
import io.vertx.core.internal.logging.LoggerFactory
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
    const val LIST_REPOSITORIES = "charter.connection.repositories.list"
  }

  private val logger = LoggerFactory.getLogger(this::class.java)

  private lateinit var webClient: WebClient

  override suspend fun start() {
    super.start()

    webClient = WebClient.create(vertx)

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(APPROVE, handler = ::approve)
      vertx.eventBus().coConsumer(LIST_REPOSITORIES, handler = ::listRepositories)
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

      OAuthProvider.dockerhub -> {
        OAuth2Auth.create(
          vertx,
          OAuth2Options()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setSite("https://hub.docker.com")
            .setAuthorizationPath("/oauth/authorize")
            .setTokenPath("/oauth/token")
            .setUserInfoPath("/api/user"),
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

        OAuthProvider.dockerhub -> {
          webClient
            .get(443, "hub.docker.com", "/v2/user")
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
    val provider =
      body.getString("provider")?.let { OAuthProvider.valueOf(it) }
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_PROVIDER_MISSING)
    val type =
      body.getString("type")?.let { OAuthConnectionType.valueOf(it) }
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_TYPE_MISSING)

    // Validate provider and type combination
    when (provider) {
      OAuthProvider.github -> {
        // GitHub can be used for both git repositories and container registry (GHCR)
        if (type != OAuthConnectionType.git && type != OAuthConnectionType.registry) {
          throw ApiStatusReplyException(ApiStatus.OAUTH_TYPE_MISMATCH)
        }
      }

      OAuthProvider.gitlab, OAuthProvider.bitbucket -> {
        // GitLab and Bitbucket are only for git repositories
        if (type != OAuthConnectionType.git) {
          throw ApiStatusReplyException(ApiStatus.OAUTH_TYPE_MISMATCH)
        }
      }

      OAuthProvider.dockerhub -> {
        // Docker Hub is only for container registry
        if (type != OAuthConnectionType.registry) {
          throw ApiStatusReplyException(ApiStatus.OAUTH_TYPE_MISMATCH)
        }
      }
    }

    val displayName = body.getString("display_name")
    val requestedScopes = body.getJsonArray("scopes") ?: JsonArray()

    // Check for existing pending connections with same vessel/charter, provider, and type
    val existingConnectionsRequest =
      EventBusQueryDataRequest(
        filters =
          filters {
            OAUTH_CONNECTIONS.CHARTER_ID eq charterId
            OAUTH_CONNECTIONS.VESSEL_ID eq vesselId
            OAUTH_CONNECTIONS.PROVIDER eq provider
            OAUTH_CONNECTIONS.TYPE eq type
            OAUTH_CONNECTIONS.STATUS eq OAuthConnectionStatus.pending
          },
        pagination = Pagination(offset = 0, limit = 100),
      )

    val existingConnections =
      vertx
        .eventBus()
        .request<EventBusDataResponse<OAuthConnections>>(OAuthConnectionDataVerticle.LIST, existingConnectionsRequest)
        .coAwait()
        .body()
        .payload
        ?.toMany() ?: emptyList()

    // Delete any existing pending connections
    for (existingConnection in existingConnections) {
      vertx
        .eventBus()
        .request<EventBusDataResponse<OAuthConnections>>(
          OAuthConnectionDataVerticle.DELETE,
          EventBusCmdDataRequest(identifier = existingConnection.id, userId = userId),
        ).coAwait()
    }

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
    val oauthConfig =
      config.getJsonObject("oauth")
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CONFIG_MISSING)

    val providerConfig =
      oauthConfig.getJsonObject(provider.literal)
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_PROVIDER_CONFIG_MISSING)

    val clientId =
      providerConfig.getString("client_id")
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CLIENT_ID_MISSING)
    val clientSecret =
      providerConfig.getString("client_secret")
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CLIENT_SECRET_MISSING)

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
        OAuth2Auth.create(
          vertx,
          OAuth2Options()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setSite("https://hub.docker.com")
            .setAuthorizationPath("/oauth/authorize")
            .setTokenPath("/oauth/token")
            .setUserInfoPath("/api/user"),
        )
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
        OAuthProvider.dockerhub -> accountInfo.getString("username")
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

  private suspend fun listRepositories(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId
    val connectionId =
      apiRequest.pathParams
        ?.get("oauthConnectionId")
        ?.string
        ?.toUUID()
        ?: throw ApiStatusReplyException(ApiStatus.ID_MISSING)

    // Parse query parameters
    val page = apiRequest.query?.get("page")?.integer ?: 1
    val perPage = apiRequest.query?.get("per_page")?.integer ?: 30
    val visibility = apiRequest.query?.get("visibility")?.string
    val archived = apiRequest.query?.get("archived")?.boolean
    val sort = apiRequest.query?.get("sort")?.string ?: "updated"
    val direction = apiRequest.query?.get("direction")?.string ?: "desc"
    val search = apiRequest.query?.get("search")?.string

    // Get the OAuth connection
    val dataRequest =
      EventBusQueryDataRequest(
        identifiers = mapOf("id" to connectionId.toString()),
        filters =
          filters {
            OAUTH_CONNECTIONS.CHARTER_ID eq charterId
            OAUTH_CONNECTIONS.VESSEL_ID eq vesselId
          },
      )

    val connection =
      vertx
        .eventBus()
        .request<EventBusDataResponse<OAuthConnections>>(OAuthConnectionDataVerticle.GET, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CONNECTION_NOT_FOUND)

    // Verify connection is active and of type 'git'
    if (connection.status != OAuthConnectionStatus.active) {
      throw ApiStatusReplyException(ApiStatus.OAUTH_CONNECTION_INACTIVE)
    }
    if (connection.type != OAuthConnectionType.git) {
      throw ApiStatusReplyException(ApiStatus.OAUTH_CONNECTION_TYPE_NOT_GIT)
    }

    // Get credentials
    val credentials = getConnectionCredentials(connectionId) ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CREDENTIALS_NOT_FOUND)
    val accessToken = credentials.getString("access_token")

    // Fetch repositories from provider
    val result =
      when (connection.provider) {
        OAuthProvider.github -> fetchGitHubRepositories(accessToken, page, perPage, visibility, archived, sort, direction, search)
        OAuthProvider.gitlab -> fetchGitLabRepositories(accessToken, page, perPage, visibility, archived, sort, direction, search)
        OAuthProvider.bitbucket -> fetchBitbucketRepositories(accessToken, page, perPage, visibility, archived, sort, direction, search)
        else -> throw ApiStatusReplyException(ApiStatus.OAUTH_CONNECTION_TYPE_NOT_GIT)
      }

    message.reply(result)
  }

  private suspend fun getConnectionCredentials(connectionId: UUID): JsonObject? =
    try {
      val encryptionKey =
        config.getJsonObject("jwt")?.getString("pepper")
          ?: throw ApiStatusReplyException(ApiStatus.JWT_PEPPER_MISSING)

      // Query credentials via DataVerticle
      val credentialsRequest =
        EventBusQueryDataRequest(
          identifiers = mapOf("connection_id" to connectionId.toString()),
        )

      val result =
        vertx
          .eventBus()
          .request<EventBusDataResponse<generated.jooq.tables.pojos.OAuthCredentials>>(
            OAuthConnectionDataVerticle.GET_CREDENTIALS,
            credentialsRequest,
          ).coAwait()
          .body()

      val credentials = result.payload?.toOne() ?: return null

      // Decrypt the credentials
      JsonObject()
        .put("access_token", Encryption.decrypt(credentials.accessTokenEncrypted, encryptionKey))
        .put("refresh_token", Encryption.decrypt(credentials.refreshTokenEncrypted, encryptionKey))
        .put("account_login", credentials.accountLogin)
    } catch (e: Exception) {
      null
    }

  private suspend fun fetchGitHubRepositories(
    accessToken: String,
    page: Int,
    perPage: Int,
    visibility: String?,
    archived: Boolean?,
    sort: String,
    direction: String,
    search: String?,
  ): EventBusApiResponse {
    try {
      // Build GitHub API URL
      var url = "/user/repos?page=$page&per_page=$perPage&sort=$sort&direction=$direction"

      // Add optional filters
      visibility?.let { url += "&visibility=$it" }

      // Make API request
      val response =
        webClient
          .get(443, "api.github.com", url)
          .ssl(true)
          .bearerTokenAuthentication(accessToken)
          .putHeader("Accept", "application/vnd.github+json")
          .putHeader("X-GitHub-Api-Version", "2022-11-28")
          .send()
          .coAwait()

      if (response.statusCode() != 200) {
        throw ApiStatusReplyException(ApiStatus.OAUTH_PROVIDER_API_ERROR)
      }

      val repos = response.bodyAsJsonArray()

      // Get pagination info from Link header
      val linkHeader = response.getHeader("Link")
      val totalCount = repos.size() // GitHub doesn't always provide total count
      val hasNextPage = linkHeader?.contains("rel=\"next\"") ?: false

      // Normalize repositories to our format
      val normalizedRepos =
        repos
          .filterIsInstance<JsonObject>()
          .filter { repo ->
            // Apply archived filter if specified
            if (archived != null) {
              repo.getBoolean("archived") == archived
            } else {
              true
            }
          }.filter { repo ->
            // Apply search filter if specified
            if (search != null) {
              repo.getString("name")?.contains(search, ignoreCase = true) == true ||
                repo.getString("full_name")?.contains(search, ignoreCase = true) == true
            } else {
              true
            }
          }.map { repo ->
            JsonObject()
              .put("id", repo.getValue("id").toString())
              .put("name", repo.getString("name"))
              .put("full_name", repo.getString("full_name"))
              .put("description", repo.getString("description"))
              .put("provider", "github")
              .put("visibility", if (repo.getBoolean("private") == true) "private" else "public")
              .put("default_branch", repo.getString("default_branch"))
              .put("html_url", repo.getString("html_url"))
              .put("language", repo.getString("language"))
              .put("stars", repo.getInteger("stargazers_count"))
              .put("forks", repo.getInteger("forks_count"))
              .put("open_issues", repo.getInteger("open_issues_count"))
              .put("created_at", repo.getString("created_at"))
              .put("updated_at", repo.getString("updated_at"))
              .put("pushed_at", repo.getString("pushed_at"))
              .put("archived", repo.getBoolean("archived"))
          }

      // Calculate total pages (rough estimate)
      val totalPages = if (hasNextPage) page + 1 else page

      return EventBusApiResponse(
        data = JsonArray(normalizedRepos),
        meta =
          JsonObject()
            .put("total", totalCount)
            .put("page", page)
            .put("per_page", perPage)
            .put("total_pages", totalPages),
        httpStatus = HttpStatus.Ok,
      )
    } catch (e: ApiStatusReplyException) {
      throw e
    } catch (e: Exception) {
      throw ApiStatusReplyException(ApiStatus.OAUTH_PROVIDER_API_ERROR)
    }
  }

  private suspend fun fetchGitLabRepositories(
    accessToken: String,
    page: Int,
    perPage: Int,
    visibility: String?,
    archived: Boolean?,
    sort: String,
    direction: String,
    search: String?,
  ): EventBusApiResponse {
    try {
      // Build GitLab API URL
      var url = "/api/v4/projects?membership=true&page=$page&per_page=$perPage"

      // Map sort parameter to GitLab's ordering
      val orderBy =
        when (sort) {
          "name" -> "name"
          "created" -> "created_at"
          "updated" -> "updated_at"
          "pushed", "stars" -> "last_activity_at"
          else -> "last_activity_at"
        }
      url += "&order_by=$orderBy&sort=$direction"

      // Add optional filters
      visibility?.let { url += "&visibility=$it" }
      archived?.let { url += "&archived=$it" }
      search?.let { url += "&search=${java.net.URLEncoder.encode(it, "UTF-8")}" }

      // Make API request
      val response =
        webClient
          .get(443, "gitlab.com", url)
          .ssl(true)
          .bearerTokenAuthentication(accessToken)
          .send()
          .coAwait()

      if (response.statusCode() != 200) {
        throw ApiStatusReplyException(ApiStatus.OAUTH_PROVIDER_API_ERROR)
      }

      val projects = response.bodyAsJsonArray()

      // Get pagination info from headers
      val totalCount = response.getHeader("X-Total")?.toIntOrNull() ?: projects.size()
      val totalPages = response.getHeader("X-Total-Pages")?.toIntOrNull() ?: page

      // Normalize projects to our format
      val normalizedRepos =
        projects
          .filterIsInstance<JsonObject>()
          .map { project ->
            JsonObject()
              .put("id", project.getValue("id").toString())
              .put("name", project.getString("name"))
              .put("full_name", project.getString("path_with_namespace"))
              .put("description", project.getString("description"))
              .put("provider", "gitlab")
              .put(
                "visibility",
                when (project.getString("visibility")) {
                  "private" -> "private"
                  "internal" -> "internal"
                  else -> "public"
                },
              ).put("default_branch", project.getString("default_branch"))
              .put("html_url", project.getString("web_url"))
              .put("language", null) // GitLab doesn't provide this in list view
              .put("stars", project.getInteger("star_count"))
              .put("forks", project.getInteger("forks_count"))
              .put("open_issues", project.getInteger("open_issues_count"))
              .put("created_at", project.getString("created_at"))
              .put("updated_at", project.getString("last_activity_at"))
              .put("pushed_at", project.getString("last_activity_at"))
              .put("archived", project.getBoolean("archived"))
          }

      return EventBusApiResponse(
        data = JsonArray(normalizedRepos),
        meta =
          JsonObject()
            .put("total", totalCount)
            .put("page", page)
            .put("per_page", perPage)
            .put("total_pages", totalPages),
        httpStatus = HttpStatus.Ok,
      )
    } catch (e: ApiStatusReplyException) {
      throw e
    } catch (e: Exception) {
      throw ApiStatusReplyException(ApiStatus.OAUTH_PROVIDER_API_ERROR)
    }
  }

  private suspend fun fetchBitbucketRepositories(
    accessToken: String,
    page: Int,
    perPage: Int,
    visibility: String?,
    archived: Boolean?,
    sort: String,
    direction: String,
    search: String?,
  ): EventBusApiResponse {
    try {
      // Build Bitbucket API URL
      // Note: Bitbucket uses pagelen instead of per_page and doesn't have 0-index pages
      var url = "/2.0/repositories?role=member&pagelen=$perPage&page=$page"

      // Map sort parameter to Bitbucket's sorting
      val sortParam =
        when (sort) {
          "name" -> "name"

          "created" -> "created_on"

          "updated" -> "updated_on"

          "pushed" -> "-updated_on"

          // Most recently pushed
          else -> "-updated_on"
        }
      url += "&sort=$sortParam"

      // Add search filter (Bitbucket uses 'q' parameter with FIQL)
      if (search != null) {
        url += "&q=name~\"${java.net.URLEncoder.encode(search, "UTF-8")}\""
      }

      // Make API request
      val response =
        webClient
          .get(443, "api.bitbucket.org", url)
          .ssl(true)
          .bearerTokenAuthentication(accessToken)
          .send()
          .coAwait()

      if (response.statusCode() != 200) {
        throw ApiStatusReplyException(ApiStatus.OAUTH_PROVIDER_API_ERROR)
      }

      val body = response.bodyAsJsonObject()
      val repos = body.getJsonArray("values") ?: JsonArray()

      // Get pagination info
      val totalCount = body.getInteger("size") ?: repos.size()
      val nextPage = body.getString("next")
      val hasNextPage = nextPage != null

      // Normalize repositories to our format
      val normalizedRepos =
        repos
          .filterIsInstance<JsonObject>()
          .filter { repo ->
            // Apply visibility filter
            if (visibility != null) {
              val isPrivate = repo.getBoolean("is_private") ?: false
              when (visibility) {
                "private" -> isPrivate
                "public" -> !isPrivate
                else -> true
              }
            } else {
              true
            }
          }.map { repo ->
            val isPrivate = repo.getBoolean("is_private") ?: false
            val links = repo.getJsonObject("links")
            val htmlUrl = links?.getJsonObject("html")?.getString("href")

            JsonObject()
              .put("id", repo.getString("uuid")?.replace("[{}]".toRegex(), ""))
              .put("name", repo.getString("name"))
              .put("full_name", repo.getString("full_name"))
              .put("description", repo.getString("description"))
              .put("provider", "bitbucket")
              .put("visibility", if (isPrivate) "private" else "public")
              .put("default_branch", repo.getJsonObject("mainbranch")?.getString("name") ?: "main")
              .put("html_url", htmlUrl)
              .put("language", repo.getString("language"))
              .put("stars", 0) // Bitbucket API v2 doesn't provide star count in list
              .put("forks", 0) // Need separate API call for fork count
              .put("open_issues", 0) // Need separate API call for issues
              .put("created_at", repo.getString("created_on"))
              .put("updated_at", repo.getString("updated_on"))
              .put("pushed_at", repo.getString("updated_on"))
              .put("archived", false) // Bitbucket doesn't have archived status in API
          }

      // Calculate total pages
      val totalPages = if (totalCount > 0) ((totalCount + perPage - 1) / perPage) else 1

      return EventBusApiResponse(
        data = JsonArray(normalizedRepos),
        meta =
          JsonObject()
            .put("total", totalCount)
            .put("page", page)
            .put("per_page", perPage)
            .put("total_pages", totalPages),
        httpStatus = HttpStatus.Ok,
      )
    } catch (e: ApiStatusReplyException) {
      throw e
    } catch (e: Exception) {
      throw ApiStatusReplyException(ApiStatus.OAUTH_PROVIDER_API_ERROR)
    }
  }
}
