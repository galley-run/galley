// Galley – https://galley.run
// Copyright (c) 2025 Clicqo
// Licensed under the Business Source License 1.1 (BUSL-1.1)
// See LICENSE.adoc for details.

package run.galley.cloud

import io.vertx.core.DeploymentOptions
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpVersion
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.net.PemKeyCertOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.HttpException
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.api.ApiStatusReplyExceptionMessageCodec
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiRequestCodec
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusApiResponseCodec
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusCmdDataRequestCodec
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusDataResponseCodec
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.eventbus.EventBusQueryDataRequestCodec
import nl.clicqo.ext.applyIf
import nl.clicqo.ext.setupCorsHandler
import nl.clicqo.ext.setupDefaultOptionsHandler
import nl.clicqo.ext.setupDefaultResponse
import nl.clicqo.ext.setupFailureHandler
import nl.clicqo.license.LicenseVerticle
import nl.clicqo.messaging.email.EmailComposer
import nl.clicqo.messaging.email.EmailComposerCodec
import nl.clicqo.messaging.email.EmailMessagingVerticle
import run.galley.cloud.controller.AuthControllerVerticle
import run.galley.cloud.controller.CharterComputePlanControllerVerticle
import run.galley.cloud.controller.CharterConnectionControllerVerticle
import run.galley.cloud.controller.CharterControllerVerticle
import run.galley.cloud.controller.ProjectControllerVerticle
import run.galley.cloud.controller.VesselBillingProfileControllerVerticle
import run.galley.cloud.controller.VesselControllerVerticle
import run.galley.cloud.controller.VesselEngineControllerVerticle
import run.galley.cloud.controller.VesselEngineNodeControllerVerticle
import run.galley.cloud.controller.VesselEngineRegionControllerVerticle
import run.galley.cloud.data.CharterComputePlanDataVerticle
import run.galley.cloud.data.CharterDataVerticle
import run.galley.cloud.data.CrewCharterMemberDataVerticle
import run.galley.cloud.data.CrewDataVerticle
import run.galley.cloud.data.OAuthConnectionDataVerticle
import run.galley.cloud.data.ProjectDataVerticle
import run.galley.cloud.data.SessionDataVerticle
import run.galley.cloud.data.UserDataVerticle
import run.galley.cloud.data.VesselBillingProfileDataVerticle
import run.galley.cloud.data.VesselDataVerticle
import run.galley.cloud.data.VesselEngineDataVerticle
import run.galley.cloud.data.VesselEngineNodeDataVerticle
import run.galley.cloud.data.VesselEngineRegionDataVerticle
import run.galley.cloud.db.FlywayMigrationVerticle
import run.galley.cloud.model.BaseModel
import run.galley.cloud.web.OpenApiBridge
import run.galley.cloud.ws.AgentWebSocketServer
import run.galley.cloud.ws.EventBusAgentRequest
import run.galley.cloud.ws.EventBusAgentRequestCodec
import run.galley.cloud.ws.EventBusAgentResponse
import run.galley.cloud.ws.EventBusAgentResponseCodec
import run.galley.cloud.ws.VesselEngineAgentTunnel
import java.util.UUID

class MainVerticle : CoroutineVerticle() {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override suspend fun start() {
    vertx.eventBus().registerDefaultCodec(EmailComposer::class.java, EmailComposerCodec())
    vertx.eventBus().registerDefaultCodec(EventBusApiRequest::class.java, EventBusApiRequestCodec())
    vertx.eventBus().registerDefaultCodec(EventBusApiResponse::class.java, EventBusApiResponseCodec())
    vertx.eventBus().registerDefaultCodec(EventBusCmdDataRequest::class.java, EventBusCmdDataRequestCodec())
    vertx.eventBus().registerDefaultCodec(EventBusQueryDataRequest::class.java, EventBusQueryDataRequestCodec())
    vertx.eventBus().registerDefaultCodec(EventBusAgentRequest::class.java, EventBusAgentRequestCodec())
    vertx.eventBus().registerDefaultCodec(EventBusAgentResponse::class.java, EventBusAgentResponseCodec())
    @Suppress("UNCHECKED_CAST")
    vertx.eventBus().registerDefaultCodec(
      EventBusDataResponse::class.java,
      EventBusDataResponseCodec<BaseModel>() as MessageCodec<EventBusDataResponse<out BaseModel>, EventBusDataResponse<out BaseModel>>,
    )
    vertx.eventBus().registerDefaultCodec(ApiStatusReplyException::class.java, ApiStatusReplyExceptionMessageCodec())

    val mainRouter =
      Router
        .router(vertx)
    mainRouter.run {
      setupCorsHandler(JsonArray().add("*"))
      setupDefaultOptionsHandler()
    }

    val openApiBridge = OpenApiBridge(vertx, config).initialize()
    val openApiRouter = openApiBridge.buildRouter().createRouter()
    openApiRouter.run {
      setupCorsHandler(config.getJsonObject("api.galley.run").getJsonArray("cors", JsonArray().add(".*")))
      setupDefaultOptionsHandler()
      setupDefaultResponse()
      setupFailureHandler()
      post().handler(BodyHandler.create())
      patch().handler(BodyHandler.create())
      put().handler(BodyHandler.create())
    }
    mainRouter.route().virtualHost(config.getJsonObject("api.galley.run").getString("host", "localhost")).subRouter(openApiRouter)

    val webAppRouter = Router.router(vertx)
    webAppRouter.run {
      route(
        "/*",
      ).handler(
        StaticHandler
          .create(
            "webroot",
          ).setCachingEnabled(false)
          .setIncludeHidden(false),
      )
      errorHandler(404) { ctx ->
        val req = ctx.request()
        if (req.headers().get("Accept")?.contains("text/html") == true) {
          ctx.response().sendFile("webroot/index.html")
        } else {
          ctx.fail(HttpException(404, "Not Found"))
        }
      }
    }
    val webAppConfig = config.getJsonObject("cloud.galley.run", JsonObject())
    mainRouter
      .route()
      .virtualHost(webAppConfig.getString("host", "localhost"))
      .subRouter(webAppRouter)

    val getAppRouter = Router.router(vertx)
    getAppRouter.run {
      route(
        "/",
      ).handler(
        StaticHandler
          .create(
            "getroot",
          ).setIndexPage("install.sh"),
      )
      route(
        "/bin/*",
      ).handler(
        StaticHandler
          .create(
            "getroot/bin",
          ).setDirectoryListing(true)
          .setCachingEnabled(false),
      )
      errorHandler(404) { ctx ->
        val req = ctx.request()
        if (req.headers().get("Accept")?.contains("text/html") == true) {
          ctx.redirect("https://galley.run")
        } else {
          ctx.fail(HttpException(404, "Not Found"))
        }
      }
    }
    val getAppConfig = config.getJsonObject("get.galley.run", JsonObject())
    mainRouter
      .route()
      .virtualHost(getAppConfig.getString("host", "localhost"))
      .subRouter(getAppRouter)

    // Deploy verticles
    val deploymentOptions =
      DeploymentOptions()
        .setConfig(config)

    // Start the DB migration
    // MainVerticle will fail to deploy if the migration fails
    val flywayMigrationVerticleId =
      vertx
        .deployVerticle(FlywayMigrationVerticle(), deploymentOptionsOf(config.getJsonObject("db")))
        .coAwait()
    // Undeploy once the migration is done
    vertx.undeploy(flywayMigrationVerticleId).coAwait()

    val emailConfig = config.getJsonObject("messaging", JsonObject()).getJsonObject("email", JsonObject())
    vertx.deployVerticle(EmailMessagingVerticle(), deploymentOptionsOf(emailConfig)).coAwait()

    vertx.deployVerticle(LicenseVerticle(), deploymentOptions).coAwait()

    // Initialize WebSocket server
    val agentWebSocketServer = AgentWebSocketServer(vertx, config.getJsonObject("_outboundAgent"))
    VesselEngineAgentTunnel(vertx, agentWebSocketServer, coroutineContext)
    // For development purpose only
    config.getJsonObject("_outboundAgent")?.forEach {
      agentWebSocketServer.createOutboundConnection(UUID.fromString(it.key))
    }

    // Setup Postgres DB Pool and deploy all data verticles
    vertx.deployVerticle(SessionDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(UserDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(CrewDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(CrewCharterMemberDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(CharterDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(CharterComputePlanDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(ProjectDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(OAuthConnectionDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(VesselDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(VesselEngineDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(VesselBillingProfileDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(VesselEngineNodeDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(VesselEngineRegionDataVerticle(), deploymentOptions).coAwait()

    // Deploy the controller verticles
    vertx.deployVerticle(AuthControllerVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(CharterControllerVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(CharterComputePlanControllerVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(CharterConnectionControllerVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(ProjectControllerVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(VesselControllerVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(VesselBillingProfileControllerVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(VesselEngineControllerVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(VesselEngineNodeControllerVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(VesselEngineRegionControllerVerticle(), deploymentOptions).coAwait()

    val httpHost =
      config
        .getJsonObject("http", JsonObject())
        .getString("host")
    val httpPort =
      config
        .getJsonObject("http", JsonObject())
        .getInteger("port", 9233)
    val certPath =
      config
        .getJsonObject("http", JsonObject())
        .getString("certPath")
    val keyPath =
      config
        .getJsonObject("http", JsonObject())
        .getString("keyPath")

    val httpServerOptions =
      HttpServerOptions()
        .applyIf(!httpHost.isNullOrBlank()) { this.setHost(httpHost) }
        .setPort(httpPort)
        .applyIf(!certPath.isNullOrBlank() && !keyPath.isNullOrBlank()) {
          this
            .setUseAlpn(true)
            .setAlpnVersions(listOf(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1))
            .setSni(true)
            .setSsl(true)
            .setKeyCertOptions(PemKeyCertOptions().setCertPath(certPath).setKeyPath(keyPath))
        }

    try {
      // Start to run the HTTP server with WebSocket support
      vertx
        .createHttpServer(httpServerOptions)
        .webSocketHandshakeHandler(agentWebSocketServer.handshakeHandler())
        .webSocketHandler(agentWebSocketServer.connectionHandler())
        .requestHandler(mainRouter)
        .listen()
        .coAwait()

      logger.info("HTTP server started on port $httpPort with WebSocket support")
    } catch (e: Exception) {
      logger.error("HTTP server failed to start on port $httpPort", e)
      throw e
    }
  }
}
