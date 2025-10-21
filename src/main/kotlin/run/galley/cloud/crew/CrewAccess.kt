package run.galley.cloud.crew

import io.vertx.core.json.JsonObject

abstract class CrewAccess {
  abstract fun toJson(): JsonObject

//  fun valueOf(val crewAccess: JsonObject): CrewAccess {
//    crewAccess
//  }
}
