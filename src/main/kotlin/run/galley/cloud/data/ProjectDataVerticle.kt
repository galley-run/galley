package run.galley.cloud.data

import generated.jooq.tables.pojos.CharterProjects
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
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
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(PATCH, handler = ::patch)
      vertx.eventBus().coConsumer(ARCHIVE, handler = ::archive)
    }
  }

  private suspend fun list(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(ProjectSql.listProjects(request))

    val projects = results?.map(ProjectFactory::from) ?: emptyList()

    val metadata =
      request.pagination?.let {
        JsonObject()
          .put("offset", it.offset)
          .put("limit", it.limit)
          .put("count", projects.size)
      }

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(projects),
        metadata = metadata,
      ),
    )
  }

  private suspend fun get(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(ProjectSql.getProject(request))

    val charter =
      results
        ?.firstOrNull()
        ?.let(ProjectFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.PROJECT_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(charter)))
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(ProjectSql.createProject(request))

    val project =
      results?.firstOrNull()?.let(ProjectFactory::from) ?: throw ApiStatusReplyException(ApiStatus.PROJECT_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(project)))
  }

  private suspend fun patch(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(ProjectSql.patchProject(request))

    val project =
      results?.firstOrNull()?.let(ProjectFactory::from) ?: throw ApiStatusReplyException(ApiStatus.PROJECT_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(project)))
  }

  private suspend fun archive(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val updated = pool.execute(ProjectSql.archiveProject(request))

    if (updated?.rowCount() == 0) {
      throw ApiStatusReplyException(ApiStatus.PROJECT_NOT_FOUND)
    }

    message.reply(EventBusDataResponse.noContent<CharterProjects>())
  }
}
