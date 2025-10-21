package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import nl.clicqo.data.DataPayload
import nl.clicqo.data.executePreparedQuery
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.model.factory.ProjectFactory
import run.galley.cloud.sql.ProjectSql

class ProjectDataVerticle : PostgresDataVerticle() {
  companion object {
    const val LIST = "data.project.query.list"
    const val GET = "data.project.query.get"
    const val CREATE = "data.project.cmd.create"
    const val PATCH = "data.project.cmd.patch"
    const val ARCHIVE = "data.project.cmd.archive"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
    }
  }

  private suspend fun list(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(ProjectSql.listProjects(request))

    val charters = results?.map(ProjectFactory::from) ?: emptyList()

    val metadata =
      request.pagination?.let {
        JsonObject()
          .put("offset", it.offset)
          .put("limit", it.limit)
          .put("count", charters.size)
      }

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(charters),
        metadata = metadata,
      ),
    )
  }
}
