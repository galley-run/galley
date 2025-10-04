package run.galley.cloud.model

import io.vertx.core.json.JsonObject
import java.util.UUID
import nl.clicqo.data.DataModel
import nl.clicqo.ext.getUUID

data class Vessel(val id: UUID? = null, val name: String? = null, val desk: String? = null) : DataModel() {
  override fun toJsonObject(): JsonObject =
    JsonObject()
      .put("id", id)
      .put("name", name)
      .put("desk", desk)

  companion object : DataModelFactory<Vessel> {
    override fun from(data: JsonObject): Vessel = Vessel(data.getUUID("id"), data.getString("name"))
    override fun from(row: io.vertx.sqlclient.Row): Vessel = Vessel(row.getUUID("id"), row.getString("name"))
  }
}
