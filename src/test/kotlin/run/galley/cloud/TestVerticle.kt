package run.galley.cloud

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.api.ApiStatusReplyExceptionMessageCodec
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataRequestCodec
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusDataResponseCodec
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
open class TestVerticle {
  companion object {
    @JvmStatic
    @BeforeAll
    fun setup(vertx: Vertx) {
      vertx.eventBus().registerDefaultCodec(EventBusDataRequest::class.java, EventBusDataRequestCodec())
      vertx.eventBus().registerDefaultCodec(EventBusDataResponse::class.java, EventBusDataResponseCodec())
      vertx.eventBus().registerDefaultCodec(ApiStatusReplyException::class.java, ApiStatusReplyExceptionMessageCodec())
    }
  }

}
