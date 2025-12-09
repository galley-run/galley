package run.galley.cloud.data

import generated.jooq.tables.pojos.CharterComputePlans
import generated.jooq.tables.records.CharterComputePlansRecord
import generated.jooq.tables.references.CHARTER_COMPUTE_PLANS
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import nl.clicqo.ext.getUUID
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.CharterComputePlanFactory
import run.galley.cloud.sql.CharterComputePlanSql

class CharterComputePlanDataVerticle : PostgresDataVerticle() {
  companion object {
    const val LIST = "data.charter.computePlan.query.list"
    const val GET = "data.charter.computePlan.query.get"
    const val CREATE = "data.charter.computePlan.cmd.create"
    const val CREATE_INITIAL = "data.charter.computePlan.cmd.create_initial"
    const val PATCH = "data.charter.computePlan.cmd.patch"
    const val ARCHIVE = "data.charter.computePlan.cmd.archive"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(CREATE_INITIAL, handler = ::createInitial)
      vertx.eventBus().coConsumer(PATCH, handler = ::patch)
      vertx.eventBus().coConsumer(ARCHIVE, handler = ::archive)
    }
  }

  private suspend fun list(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(CharterComputePlanSql.listComputePlans(request))

    val computePlans = results?.map(CharterComputePlanFactory::from) ?: emptyList()

    val metadata =
      request.pagination?.let {
        JsonObject()
          .put("offset", it.offset)
          .put("limit", it.limit)
          .put("count", computePlans.size)
      }

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(computePlans),
        metadata = metadata,
      ),
    )
  }

  private suspend fun get(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(CharterComputePlanSql.getComputePlan(request))

    val computePlan =
      results
        ?.firstOrNull()
        ?.let(CharterComputePlanFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.COMPUTE_PLAN_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(computePlan)))
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(CharterComputePlanSql.createComputePlan(request))

    val computePlan =
      results?.firstOrNull()?.let(CharterComputePlanFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.COMPUTE_PLAN_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(computePlan)))
  }

  private suspend fun createInitial(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()

    val vesselId =
      request.payload?.getUUID(CHARTER_COMPUTE_PLANS.VESSEL_ID.name) ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ID_INCORRECT)
    val charterId =
      request.payload.getUUID(CHARTER_COMPUTE_PLANS.CHARTER_ID.name) ?: throw ApiStatusReplyException(ApiStatus.CHARTER_ID_INCORRECT)

    val computePlans =
      listOf(
        CharterComputePlansRecord(
          vesselId = vesselId,
          charterId = charterId,
          name = "Compute XS",
          requestsCpu = "0.25",
          requestsMemory = "256Mi",
          limitsCpu = "0.25",
          limitsMemory = "256Mi",
          application = "applications_databases",
          billingEnabled = false,
        ),
        CharterComputePlansRecord(
          vesselId = vesselId,
          charterId = charterId,
          name = "Compute S",
          requestsCpu = "0.5",
          requestsMemory = "512Mi",
          limitsCpu = "0.5",
          limitsMemory = "512Mi",
          application = "applications_databases",
          billingEnabled = false,
        ),
        CharterComputePlansRecord(
          vesselId = vesselId,
          charterId = charterId,
          name = "Compute M",
          requestsCpu = "1",
          requestsMemory = "1Gi",
          limitsCpu = "1",
          limitsMemory = "1Gi",
          application = "applications_databases",
          billingEnabled = false,
        ),
        CharterComputePlansRecord(
          vesselId = vesselId,
          charterId = charterId,
          name = "Compute L",
          requestsCpu = "2",
          requestsMemory = "2Gi",
          limitsCpu = "2",
          limitsMemory = "2Gi",
          application = "applications_databases",
          billingEnabled = false,
        ),
        CharterComputePlansRecord(
          vesselId = vesselId,
          charterId = charterId,
          name = "Compute XL",
          requestsCpu = "4",
          requestsMemory = "4Gi",
          limitsCpu = "4",
          limitsMemory = "4Gi",
          application = "applications_databases",
          billingEnabled = false,
        ),
      )

    val results = pool.execute(CharterComputePlanSql.createComputePlans(computePlans))

    message.reply(results?.rowCount() == 5)
  }

  private suspend fun patch(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(CharterComputePlanSql.patchComputePlan(request))

    val computePlan =
      results?.firstOrNull()?.let(CharterComputePlanFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.COMPUTE_PLAN_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(computePlan)))
  }

  private suspend fun archive(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val updated = pool.execute(CharterComputePlanSql.archiveComputePlan(request))

    if (updated?.rowCount() == 0) {
      throw ApiStatusReplyException(ApiStatus.COMPUTE_PLAN_NOT_FOUND)
    }

    message.reply(EventBusDataResponse.noContent<CharterComputePlans>())
  }
}
