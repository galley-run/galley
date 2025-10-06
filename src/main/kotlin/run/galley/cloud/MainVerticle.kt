// Galley – https://galley.run
// Copyright (c) 2025 Clicqo
// Licensed under the Business Source License 1.1 (BUSL-1.1)
// See LICENSE.adoc for details.

package run.galley.cloud

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.api.ApiStatusReplyExceptionMessageCodec
import nl.clicqo.ext.setupCorsHandler
import nl.clicqo.ext.setupDefaultOptionsHandler
import nl.clicqo.ext.setupDefaultResponse
import nl.clicqo.ext.setupFailureHandler
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataRequestCodec
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusDataResponseCodec
import org.slf4j.LoggerFactory
import run.galley.cloud.controller.VesselControllerVerticle
import run.galley.cloud.db.FlywayMigrationVerticle
import run.galley.cloud.web.OpenApiBridge

class MainVerticle : CoroutineVerticle() {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override suspend fun start() {
    val configRetriever = ConfigRetriever.create(
      vertx, ConfigRetrieverOptions()
        .addStore(ConfigStoreOptions().setType("file").setConfig(JsonObject().put("path", "config.json")))
    )
    val config = configRetriever.config.coAwait()

    vertx.eventBus().registerDefaultCodec(EventBusDataRequest::class.java, EventBusDataRequestCodec())
    vertx.eventBus().registerDefaultCodec(EventBusDataResponse::class.java, EventBusDataResponseCodec())
    vertx.eventBus().registerDefaultCodec(ApiStatusReplyException::class.java, ApiStatusReplyExceptionMessageCodec())

    val openApiBridge = OpenApiBridge(vertx, config).initialize()
    val router = openApiBridge.buildRouter()
      .createRouter()
      .setupCorsHandler(config)
      .setupDefaultResponse()
      .setupDefaultOptionsHandler()
      .setupFailureHandler()

    // @TODO: Delete this line, temp JWT generation for testing
    println(openApiBridge.authProvider.generateToken(JsonObject().put("scope", "vesselCaptain")))

    // Deploy verticles
    val deploymentOptions = DeploymentOptions()
      .setConfig(config)

    // Start the DB migration
    // MainVerticle will fail to deploy if the migration fails
    val flywayMigrationVerticleId = vertx
      .deployVerticle(FlywayMigrationVerticle(), deploymentOptions)
      .coAwait()
    // Undeploy once the migration is done
    vertx.undeploy(flywayMigrationVerticleId).coAwait()

    vertx
      .deployVerticle(VesselControllerVerticle(), deploymentOptions)
      .coAwait()

    val httpPort = config
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
