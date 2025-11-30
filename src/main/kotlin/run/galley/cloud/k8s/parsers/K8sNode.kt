package run.galley.cloud.k8s.parsers

import io.vertx.core.json.JsonObject

data class K8sNode(
  val name: String?,
  val ip: String?,
  val cpu: String?,
  val memory: String?,
  val storage: String?,
  val createdAt: String?,
  val osMetadata: K8sNodeOsMetadata,
)

data class K8sNodeOsMetadata(
  val os: String?,
  val arch: String?,
  val distro: String?,
  val version: String?,
  val storageUsed: Long?,
  val memoryUsed: Long?,
  val cpuUsed: Float?,
) {
  fun toJsonObject(): JsonObject = JsonObject.mapFrom(this)
}
