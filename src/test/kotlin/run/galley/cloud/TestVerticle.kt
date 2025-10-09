package run.galley.cloud

import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import run.galley.cloud.model.BaseModel

@ExtendWith(VertxExtension::class)
open class TestVerticle {
  val config =
    JsonObject(
      """{
  "http": {
    "port": 9233
  },
  "jwt": {
    "secret": "secret",
    "keystore": "keystore.jceks",
    "type": "jceks",
    "pepper": "long-and-random-string"
  },
  "api": {
    "openApiFile": "openapi.yaml",
    "cors": [".*"]
  },
  "db": {
    "port": 5432,
    "database": "galley",
    "host": "localhost",
    "pool_size": 5,
    "username": "username",
    "password": "password"
  }
}""",
    )

  companion object {
    @JvmStatic
    @BeforeAll
    fun setup(vertx: Vertx) {
      vertx.eventBus().registerDefaultCodec(EventBusApiRequest::class.java, EventBusApiRequestCodec())
      vertx.eventBus().registerDefaultCodec(EventBusApiResponse::class.java, EventBusApiResponseCodec())
      vertx.eventBus().registerDefaultCodec(EventBusQueryDataRequest::class.java, EventBusQueryDataRequestCodec())
      @Suppress("UNCHECKED_CAST")
      vertx.eventBus().registerDefaultCodec(
        EventBusDataResponse::class.java,
        EventBusDataResponseCodec<BaseModel>() as MessageCodec<EventBusDataResponse<out BaseModel>, EventBusDataResponse<out BaseModel>>,
      )
      @Suppress("UNCHECKED_CAST")
      vertx.eventBus().registerDefaultCodec(
        EventBusCmdDataRequest::class.java,
        EventBusCmdDataRequestCodec<BaseModel>()
          as MessageCodec<EventBusCmdDataRequest<out BaseModel>, EventBusCmdDataRequest<out BaseModel>>,
      )
      vertx.eventBus().registerDefaultCodec(ApiStatusReplyException::class.java, ApiStatusReplyExceptionMessageCodec())
    }
  }
}
