package run.galley.cloud.k8s.parsers

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import nl.clicqo.ext.toBytes

class K8sParserNodeList(
  val payload: JsonObject,
) {
  val nodes: List<K8sNode>
    get() =
      payload.getJsonArray("items").map {
        it as JsonObject

        val metadata = it.getJsonObject("metadata", JsonObject())
        val status = it.getJsonObject("status", JsonObject())
        val addresses = status.getJsonArray("addresses", JsonArray())
        val ip =
          addresses.find { address ->
            address as JsonObject
            address.getString("type") == "InternalIP"
          } as JsonObject?

        val nodeCapacity = status?.getJsonObject("capacity")
        val nodeAllocated = status?.getJsonObject("allocated")
        val storageAllocated = nodeAllocated?.getString("ephemeral-storage")?.toBytes()
        val storageUsed =
          storageAllocated?.let { storageAllocated ->
            nodeCapacity?.getString("ephemeral-storage")?.toBytes()?.minus(storageAllocated)
          }
        val memoryAllocated = nodeAllocated?.getString("memory")?.toBytes()
        val memoryUsed =
          memoryAllocated?.let { memoryAllocated ->
            nodeCapacity?.getString("memory")?.toBytes()?.minus(memoryAllocated)
          }
        val cpuAllocated = nodeAllocated?.getString("cpu")?.toFloatOrNull()
        val cpuUsed =
          cpuAllocated?.let { cpuAllocated -> nodeCapacity?.getString("cpu")?.toFloatOrNull()?.minus(cpuAllocated) }

        return@map K8sNode(
          name = metadata?.getString("name"),
          ip = ip?.getString("address"),
          cpu = status?.getJsonObject("capacity")?.getString("cpu"),
          memory = status?.getJsonObject("capacity")?.getString("memory"),
          storage = status?.getJsonObject("capacity")?.getString("ephemeral-storage"),
          createdAt = metadata?.getString("creationTimestamp"),
          osMetadata =
            K8sNodeOsMetadata(
              os = status?.getJsonObject("nodeInfo")?.getString("osImage"),
              arch = status?.getJsonObject("nodeInfo")?.getString("architecture"),
              distro =
                status
                  ?.getJsonObject("nodeInfo")
                  ?.getString("osImage")
                  ?.split(" ")
                  ?.first(),
              version = null,
              storageUsed = storageUsed,
              memoryUsed = memoryUsed,
              cpuUsed = cpuUsed,
            ),
        )
      }
}
