package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.VesselEngineFactory
import run.galley.cloud.sql.VesselEngineSql
import run.galley.cloud.ws.EventBusAgentRequest
import run.galley.cloud.ws.VesselEngineAgentTunnel

class VesselEngineDataVerticle : PostgresDataVerticle() {
  companion object {
    const val CREATE = "data.vessel.engine.cmd.create"
    const val PATCH = "data.vessel.engine.cmd.patch"
    const val LIST_BY_VESSEL_ID = "data.vessel.engine.query.list_by_vessel_id"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(PATCH, handler = ::patch)
      vertx.eventBus().coConsumer(LIST_BY_VESSEL_ID, handler = ::listByVesselId)
    }
  }

  private suspend fun listByVesselId(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineSql.getByVesselId(request))

    val vesselEngines = results?.map(VesselEngineFactory::from)

    val testNamespace =
      JsonObject()
        .put("apiVersion", "v1")
        .put("kind", "Namespace")
        .put(
          "metadata",
          JsonObject()
            .put("name", "galley-test")
            .put(
              "labels",
              JsonObject().put("purpose", "galley-tooling-test"),
            ),
        )

    val testDeployment: JsonObject =
      JsonObject()
        .put("apiVersion", "apps/v1")
        .put("kind", "Deployment")
        .put(
          "metadata",
          JsonObject()
            .put("name", "galley-test-deployment")
            .put("namespace", "galley-test")
            .put(
              "labels",
              JsonObject()
                .put("app", "galley-test"),
            ),
        ).put(
          "spec",
          JsonObject()
            .put("replicas", 1)
            .put(
              "selector",
              JsonObject()
                .put(
                  "matchLabels",
                  JsonObject()
                    .put("app", "galley-test"),
                ),
            ).put(
              "template",
              JsonObject()
                .put(
                  "metadata",
                  JsonObject()
                    .put(
                      "labels",
                      JsonObject()
                        .put("app", "galley-test"),
                    ),
                ).put(
                  "spec",
                  JsonObject()
                    .put(
                      "containers",
                      JsonArray().add(
                        JsonObject()
                          .put("name", "galley-test")
                          .put("image", "nginx:1.27-alpine")
                          .put(
                            "ports",
                            JsonArray().add(
                              JsonObject()
                                .put("containerPort", 80),
                            ),
                          ),
                      ),
                    ),
                ),
            ),
        )

    vesselEngines?.forEach { vesselEngine ->
      // Sync nodes from cluster
      vertx.eventBus().send(
        VesselEngineAgentTunnel.APPLY,
        EventBusAgentRequest(
          vesselEngineId = vesselEngine.id!!,
          payload =
            JsonObject()
              .put(
                "manifests",
                JsonArray()
                  .add(testNamespace)
                  .add(testDeployment),
              ),
          replyTo = VesselEngineNodeDataVerticle.APPLIED,
        ),
      )
    }

    vesselEngines?.forEach { vesselEngine ->
      // Sync nodes from cluster
      vertx.eventBus().send(
        VesselEngineAgentTunnel.GET_NODES,
        EventBusAgentRequest(
          vesselEngineId = vesselEngine.id!!,
          payload = JsonObject(),
          replyTo = VesselEngineNodeDataVerticle.SYNC_NODES,
        ),
      )
    }

    message.reply(EventBusDataResponse(DataPayload.many(vesselEngines)))
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineSql.create(request))

    val vesselEngine =
      results?.firstOrNull()?.let(VesselEngineFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngine)))
  }

  private suspend fun patch(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineSql.patch(request))

    val vesselEngine =
      results?.firstOrNull()?.let(VesselEngineFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngine)))
  }
}
