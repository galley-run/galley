package run.galley.cloud.k8s.parsers

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.time.OffsetDateTime

class K8sParserNodeList(
  val payload: JsonObject,
) {
  val nodes: JsonArray
    get() =
      JsonArray(
        payload.getJsonArray("items").map {
          it as JsonObject
          val metadata = it.getJsonObject("metadata", JsonObject())
          val status = it.getJsonObject("status", JsonObject())
          val addresses = status.getJsonArray("addresses", JsonArray())
          val ip =
            addresses.find {
              it as JsonObject
              it.getString("type") == "InternalIP"
            } as JsonObject?

          return@map JsonObject()
            .put("name", metadata.getString("name"))
            .put("createdAt", metadata.getString("creationTimestamp"))
            .put("ip", ip?.getString("address"))
            .put("cpu", status.getJsonObject("allocatable", JsonObject()).getString("cpu"))
            .put("memory", status.getJsonObject("allocatable", JsonObject()).getString("memory"))
        },
      )
  /**
   * {
   *     "kind": "NodeList",
   *     "apiVersion": "v1",
   *     "metadata": {
   *         "resourceVersion": "281921"
   *     },
   *     "items": [
   *         {
   *             "metadata": {
   *                 "name": "worker-1",
   *                 "uid": "3e183817-127a-4af7-9892-f27beb940d98",
   *                 "resourceVersion": "281640",
   *                 "creationTimestamp": "2025-11-07T15:29:45Z",
   *                 "labels": {
   *                     "beta.kubernetes.io/arch": "arm64",
   *                     "beta.kubernetes.io/os": "linux",
   *                     "kubernetes.io/arch": "arm64",
   *                     "kubernetes.io/hostname": "worker-1",
   *                     "kubernetes.io/os": "linux"
   *                 },
   *                 "annotations": {
   *                     "node.alpha.kubernetes.io/ttl": "0",
   *                     "volumes.kubernetes.io/controller-managed-attach-detach": "true"
   *                 },
   *                 "managedFields": [
   *                     {
   *                         "manager": "kubelet",
   *                         "operation": "Update",
   *                         "apiVersion": "v1",
   *                         "time": "2025-11-07T15:29:45Z",
   *                         "fieldsType": "FieldsV1",
   *                         "fieldsV1": {
   *                             "f:metadata": {
   *                                 "f:annotations": {
   *                                     ".": {},
   *                                     "f:volumes.kubernetes.io/controller-managed-attach-detach": {}
   *                                 },
   *                                 "f:labels": {
   *                                     ".": {},
   *                                     "f:beta.kubernetes.io/arch": {},
   *                                     "f:beta.kubernetes.io/os": {},
   *                                     "f:kubernetes.io/arch": {},
   *                                     "f:kubernetes.io/hostname": {},
   *                                     "f:kubernetes.io/os": {}
   *                                 }
   *                             }
   *                         }
   *                     },
   *                     {
   *                         "manager": "kube-controller-manager",
   *                         "operation": "Update",
   *                         "apiVersion": "v1",
   *                         "time": "2025-11-08T11:05:57Z",
   *                         "fieldsType": "FieldsV1",
   *                         "fieldsV1": {
   *                             "f:metadata": {
   *                                 "f:annotations": {
   *                                     "f:node.alpha.kubernetes.io/ttl": {}
   *                                 }
   *                             },
   *                             "f:spec": {
   *                                 "f:podCIDR": {},
   *                                 "f:podCIDRs": {
   *                                     ".": {},
   *                                     "v:\"10.244.0.0/24\"": {}
   *                                 }
   *                             }
   *                         }
   *                     },
   *                     {
   *                         "manager": "kubelet",
   *                         "operation": "Update",
   *                         "apiVersion": "v1",
   *                         "time": "2025-11-10T16:01:22Z",
   *                         "fieldsType": "FieldsV1",
   *                         "fieldsV1": {
   *                             "f:status": {
   *                                 "f:allocatable": {
   *                                     "f:cpu": {},
   *                                     "f:memory": {}
   *                                 },
   *                                 "f:capacity": {
   *                                     "f:cpu": {},
   *                                     "f:memory": {}
   *                                 },
   *                                 "f:conditions": {
   *                                     "k:{\"type\":\"DiskPressure\"}": {
   *                                         "f:lastHeartbeatTime": {},
   *                                         "f:lastTransitionTime": {},
   *                                         "f:message": {},
   *                                         "f:reason": {},
   *                                         "f:status": {}
   *                                     },
   *                                     "k:{\"type\":\"MemoryPressure\"}": {
   *                                         "f:lastHeartbeatTime": {},
   *                                         "f:lastTransitionTime": {},
   *                                         "f:message": {},
   *                                         "f:reason": {},
   *                                         "f:status": {}
   *                                     },
   *                                     "k:{\"type\":\"PIDPressure\"}": {
   *                                         "f:lastHeartbeatTime": {},
   *                                         "f:lastTransitionTime": {},
   *                                         "f:message": {},
   *                                         "f:reason": {},
   *                                         "f:status": {}
   *                                     },
   *                                     "k:{\"type\":\"Ready\"}": {
   *                                         "f:lastHeartbeatTime": {},
   *                                         "f:lastTransitionTime": {},
   *                                         "f:message": {},
   *                                         "f:reason": {},
   *                                         "f:status": {}
   *                                     }
   *                                 },
   *                                 "f:images": {},
   *                                 "f:nodeInfo": {
   *                                     "f:bootID": {},
   *                                     "f:containerRuntimeVersion": {}
   *                                 }
   *                             }
   *                         },
   *                         "subresource": "status"
   *                     }
   *                 ]
   *             },
   *             "spec": {
   *                 "podCIDR": "10.244.0.0/24",
   *                 "podCIDRs": [
   *                     "10.244.0.0/24"
   *                 ]
   *             },
   *             "status": {
   *                 "capacity": {
   *                     "cpu": "4",
   *                     "ephemeral-storage": "14143556Ki",
   *                     "hugepages-1Gi": "0",
   *                     "hugepages-2Mi": "0",
   *                     "hugepages-32Mi": "0",
   *                     "hugepages-64Ki": "0",
   *                     "memory": "3989484Ki",
   *                     "pods": "110"
   *                 },
   *                 "allocatable": {
   *                     "cpu": "4",
   *                     "ephemeral-storage": "13034701189",
   *                     "hugepages-1Gi": "0",
   *                     "hugepages-2Mi": "0",
   *                     "hugepages-32Mi": "0",
   *                     "hugepages-64Ki": "0",
   *                     "memory": "3887084Ki",
   *                     "pods": "110"
   *                 },
   *                 "conditions": [
   *                     {
   *                         "type": "MemoryPressure",
   *                         "status": "False",
   *                         "lastHeartbeatTime": "2025-11-10T16:01:22Z",
   *                         "lastTransitionTime": "2025-11-09T20:31:28Z",
   *                         "reason": "KubeletHasSufficientMemory",
   *                         "message": "kubelet has sufficient memory available"
   *                     },
   *                     {
   *                         "type": "DiskPressure",
   *                         "status": "False",
   *                         "lastHeartbeatTime": "2025-11-10T16:01:22Z",
   *                         "lastTransitionTime": "2025-11-09T20:31:28Z",
   *                         "reason": "KubeletHasNoDiskPressure",
   *                         "message": "kubelet has no disk pressure"
   *                     },
   *                     {
   *                         "type": "PIDPressure",
   *                         "status": "False",
   *                         "lastHeartbeatTime": "2025-11-10T16:01:22Z",
   *                         "lastTransitionTime": "2025-11-09T20:31:28Z",
   *                         "reason": "KubeletHasSufficientPID",
   *                         "message": "kubelet has sufficient PID available"
   *                     },
   *                     {
   *                         "type": "Ready",
   *                         "status": "True",
   *                         "lastHeartbeatTime": "2025-11-10T16:01:22Z",
   *                         "lastTransitionTime": "2025-11-09T20:31:28Z",
   *                         "reason": "KubeletReady",
   *                         "message": "kubelet is posting ready status"
   *                     }
   *                 ],
   *                 "addresses": [
   *                     {
   *                         "type": "InternalIP",
   *                         "address": "192.168.2.18"
   *                     },
   *                     {
   *                         "type": "Hostname",
   *                         "address": "worker-1"
   *                     }
   *                 ],
   *                 "daemonEndpoints": {
   *                     "kubeletEndpoint": {
   *                         "Port": 10250
   *                     }
   *                 },
   *                 "nodeInfo": {
   *                     "machineID": "fbbd72056249490086cd56381629b9e1",
   *                     "systemUUID": "fbbd72056249490086cd56381629b9e1",
   *                     "bootID": "89472b97-5f52-4e85-a35a-2286b244d083",
   *                     "kernelVersion": "6.17.0-5-generic",
   *                     "osImage": "Ubuntu Resolute Raccoon (development branch)",
   *                     "containerRuntimeVersion": "containerd://1.7.28",
   *                     "kubeletVersion": "v1.34.1+k0s",
   *                     "kubeProxyVersion": "",
   *                     "operatingSystem": "linux",
   *                     "architecture": "arm64"
   *                 },
   *                 "images": [
   *                     {
   *                         "names": [
   *                             "ghcr.io/galley-run/agent@sha256:c4bc7a2ffa7973cb2a38d2a780a70d8c52842660ac94c291cfc5105fe764d86f",
   *                             "ghcr.io/galley-run/agent:edge"
   *                         ],
   *                         "sizeBytes": 108526427
   *                     },
   *                     {
   *                         "names": [
   *                             "reg.galley.dev:5999/galley-run/agent@sha256:46174aa282f5729a137d1250ae7fc9c71d0fa4c0980af7f4ab01024c4124729e"
   *                         ],
   *                         "sizeBytes": 108526337
   *                     },
   *                     {
   *                         "names": [
   *                             "reg.galley.dev:5999/galley-run/agent@sha256:db9d29cf2dea2f4d92fc978c07905aacff259fa1c1d7b7c75b1e9ed1da4cdbbd",
   *                             "reg.galley.dev:5999/galley-run/agent:dev"
   *                         ],
   *                         "sizeBytes": 108526337
   *                     },
   *                     {
   *                         "names": [
   *                             "reg.galley.dev:5999/galley-run/agent@sha256:950ec2d61ab6ef51bd5cf9aefa745dd0149e539c13fee2477cb97dad5247e933"
   *                         ],
   *                         "sizeBytes": 108526039
   *                     },
   *                     {
   *                         "names": [
   *                             "ghcr.io/galley-run/agent@sha256:e0afa7cb6fedf79389daec88e7472abe5110023c5c541799b89ef4cfe0a73ad6"
   *                         ],
   *                         "sizeBytes": 108525723
   *                     },
   *                     {
   *                         "names": [
   *                             "ghcr.io/galley-run/agent@sha256:cb51be9e5c9b67f98ba56f4de1402206b58f13df041a0dfe425f7252505b06b2",
   *                             "ghcr.io/galley-run/agent:sha-d63054a"
   *                         ],
   *                         "sizeBytes": 108525664
   *                     },
   *                     {
   *                         "names": [
   *                             "ghcr.io/galley-run/agent@sha256:80b0c03b4c8ac59247c6b9c1dbe952342444ee639db73ce74d2152364540a7a7",
   *                             "ghcr.io/galley-run/agent:sha-fbcf3a5"
   *                         ],
   *                         "sizeBytes": 108525660
   *                     },
   *                     {
   *                         "names": [
   *                             "ghcr.io/galley-run/agent@sha256:98b7f49c2b5dacf17dc1418ba46fd071b96091110f4337a7a4218980a95d13f3"
   *                         ],
   *                         "sizeBytes": 108525645
   *                     },
   *                     {
   *                         "names": [
   *                             "ghcr.io/galley-run/agent@sha256:d528b767da66a8c662af6c45692fb3915ef9c45f2bb45e1af3b65f9a655aa610",
   *                             "ghcr.io/galley-run/agent:sha-d83305b"
   *                         ],
   *                         "sizeBytes": 108525642
   *                     },
   *                     {
   *                         "names": [
   *                             "ghcr.io/galley-run/agent@sha256:7b927e2e8fe86b51b7df27c0250021011f4ccdbbd49a918d5cc4c89f5d52b14f",
   *                             "ghcr.io/galley-run/agent:sha-097bf4c"
   *                         ],
   *                         "sizeBytes": 108525483
   *                     },
   *                     {
   *                         "names": [
   *                             "quay.io/k0sproject/kube-router@sha256:37413179e6395c5a394eef764e6edacc9bae97326cfed3da58b15d7a49577a86",
   *                             "quay.io/k0sproject/kube-router:v2.6.1-iptables1.8.11-0"
   *                         ],
   *                         "sizeBytes": 33239669
   *                     },
   *                     {
   *                         "names": [
   *                             "quay.io/k0sproject/cni-node@sha256:36b7c7adff10443d659aec184d0188e252a74ecbcdbefb5269f0d20d96704800",
   *                             "quay.io/k0sproject/cni-node:1.8.0-k0s.0"
   *                         ],
   *                         "sizeBytes": 29998326
   *                     },
   *                     {
   *                         "names": [
   *                             "quay.io/k0sproject/coredns@sha256:3a685ca5adb65420ed8b1cabe848632025ec91f414db634bc7eae0af2787d6d4",
   *                             "quay.io/k0sproject/coredns:1.13.1"
   *                         ],
   *                         "sizeBytes": 20446477
   *                     },
   *                     {
   *                         "names": [
   *                             "quay.io/k0sproject/kube-proxy@sha256:65cdbf1bc2b6bf556adb42cd01be7b79712e8d5796bd5865916431c9b77ced42",
   *                             "quay.io/k0sproject/kube-proxy:v1.34.1"
   *                         ],
   *                         "sizeBytes": 19461545
   *                     },
   *                     {
   *                         "names": [
   *                             "quay.io/k0sproject/metrics-server@sha256:30448389455ab61c1373882fe9a2620c4c238b9150c4adee49ff736bd4c40740",
   *                             "quay.io/k0sproject/metrics-server:v0.7.2-0"
   *                         ],
   *                         "sizeBytes": 17013783
   *                     },
   *                     {
   *                         "names": [
   *                             "quay.io/k0sproject/apiserver-network-proxy-agent@sha256:a80026cc450fb3ab02cfde6fb3c57ce2dff156c9359706490f25692656b47952",
   *                             "quay.io/k0sproject/apiserver-network-proxy-agent:v0.33.0"
   *                         ],
   *                         "sizeBytes": 13872208
   *                     },
   *                     {
   *                         "names": [
   *                             "docker.io/library/registry@sha256:a3d8aaa63ed8681a604f1dea0aa03f100d5895b6a58ace528858a7b332415373",
   *                             "docker.io/library/registry:2"
   *                         ],
   *                         "sizeBytes": 9478682
   *                     },
   *                     {
   *                         "names": [
   *                             "quay.io/k0sproject/pause@sha256:aea0552a7e49596ce610e4ef9a7ecc2304e6ba7c76d8fdbea628c48c1d515347",
   *                             "quay.io/k0sproject/pause:3.10.1"
   *                         ],
   *                         "sizeBytes": 25697
   *                     }
   *                 ]
   *             }
   *         }
   *     ]
   * }
   */
}
