// Galley – https://galley.run
// Copyright (c) 2025 Clicqo
// Licensed under the Business Source License 1.1 (BUSL-1.1)
// See LICENSE.adoc for details.

package run.galley.cloud

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.BodyHandler
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
import org.slf4j.LoggerFactory
import run.galley.cloud.controller.AuthControllerVerticle
import run.galley.cloud.controller.CharterControllerVerticle
import run.galley.cloud.data.CharterDataVerticle
import run.galley.cloud.data.CrewDataVerticle
import run.galley.cloud.data.UserDataVerticle
import run.galley.cloud.db.FlywayMigrationVerticle
import run.galley.cloud.model.BaseModel
import run.galley.cloud.web.OpenApiBridge

class MainVerticle : CoroutineVerticle() {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override suspend fun start() {
    val configRetriever =
      ConfigRetriever.create(
        vertx,
        ConfigRetrieverOptions()
          .applyIf(vertx.fileSystem().exists("config.json").coAwait()) {
            this.addStore(ConfigStoreOptions().setType("file").setConfig(JsonObject().put("path", "config.json")))
          },
      )
    val config = configRetriever.config.coAwait().mergeIn(config)

    vertx.eventBus().registerDefaultCodec(EventBusApiRequest::class.java, EventBusApiRequestCodec())
    vertx.eventBus().registerDefaultCodec(EventBusApiResponse::class.java, EventBusApiResponseCodec())
    vertx.eventBus().registerDefaultCodec(EventBusCmdDataRequest::class.java, EventBusCmdDataRequestCodec())
    vertx.eventBus().registerDefaultCodec(EventBusQueryDataRequest::class.java, EventBusQueryDataRequestCodec())
    @Suppress("UNCHECKED_CAST")
    vertx.eventBus().registerDefaultCodec(
      EventBusDataResponse::class.java,
      EventBusDataResponseCodec<BaseModel>() as MessageCodec<EventBusDataResponse<out BaseModel>, EventBusDataResponse<out BaseModel>>,
    )
    vertx.eventBus().registerDefaultCodec(ApiStatusReplyException::class.java, ApiStatusReplyExceptionMessageCodec())

    val openApiBridge = OpenApiBridge(vertx, config).initialize()
    val router = openApiBridge.buildRouter().createRouter()
    router.run {
      setupCorsHandler(config)
      setupDefaultOptionsHandler()
      setupDefaultResponse()
      setupFailureHandler()
      post().handler(BodyHandler.create())
      patch().handler(BodyHandler.create())
      put().handler(BodyHandler.create())
    }

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

    // Setup Postgres DB Pool and deploy all data verticles
    vertx.deployVerticle(UserDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(CrewDataVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(CharterDataVerticle(), deploymentOptions).coAwait()

    // Deploy the controller verticles
    vertx.deployVerticle(CharterControllerVerticle(), deploymentOptions).coAwait()
    vertx.deployVerticle(AuthControllerVerticle(), deploymentOptions).coAwait()

    val httpPort =
      config
        .getJsonObject("http", JsonObject())
        .getInteger("port", 9233)

    try {
      // Start to run the HTTP server
      vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(httpPort)
        .coAwait()

      logger.info("HTTP server started on port $httpPort")
    } catch (e: Exception) {
      logger.error("HTTP server failed to start on port $httpPort", e)
      throw e
    }
  }
}
