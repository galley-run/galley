package run.galley.cloud.k8s

import io.vertx.core.json.JsonObject

/**
 * A Galley representation of a Kubernetes deployment spec
 */
data class DeploymentSpec(
  val replicas: Int? = 1,
  val strategy: DeploymentStrategy? = null,
  val minReadySeconds: Int? = null,
  val revisionHistoryLimit: Int? = null,
  val progressDeadlineSeconds: Int? = null,
  val autoscaling: AutoscalingSpec? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      replicas?.let { put("replicas", it) }
      strategy?.let { put("strategy", it.toJson()) }
      minReadySeconds?.let { put("minReadySeconds", it) }
      revisionHistoryLimit?.let { put("revisionHistoryLimit", it) }
      progressDeadlineSeconds?.let { put("progressDeadlineSeconds", it) }
      autoscaling?.let { put("autoscaling", it.toJson()) }
    }

  companion object {
    fun fromJson(json: JsonObject): DeploymentSpec =
      DeploymentSpec(
        replicas = json.getInteger("replicas"),
        strategy = json.getJsonObject("strategy")?.let { DeploymentStrategy.fromJson(it) },
        minReadySeconds = json.getInteger("minReadySeconds"),
        revisionHistoryLimit = json.getInteger("revisionHistoryLimit"),
        progressDeadlineSeconds = json.getInteger("progressDeadlineSeconds"),
        autoscaling = json.getJsonObject("autoscaling")?.let { AutoscalingSpec.fromJson(it) },
      )
  }
}

data class DeploymentStrategy(
  val type: StrategyType,
  val rollingUpdate: RollingUpdateSpec? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("type", type.name)
      rollingUpdate?.let { put("rollingUpdate", it.toJson()) }
    }

  companion object {
    fun fromJson(json: JsonObject): DeploymentStrategy =
      DeploymentStrategy(
        type = StrategyType.valueOf(json.getString("type")),
        rollingUpdate = json.getJsonObject("rollingUpdate")?.let { RollingUpdateSpec.fromJson(it) },
      )
  }
}

enum class StrategyType {
  RollingUpdate,
  Recreate,
}

data class RollingUpdateSpec(
  val maxUnavailable: String? = null,
  val maxSurge: String? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      maxUnavailable?.let { put("maxUnavailable", it) }
      maxSurge?.let { put("maxSurge", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): RollingUpdateSpec =
      RollingUpdateSpec(
        maxUnavailable = json.getString("maxUnavailable"),
        maxSurge = json.getString("maxSurge"),
      )
  }
}

data class AutoscalingSpec(
  val enabled: Boolean,
  val minReplicas: Int,
  val maxReplicas: Int,
  val targetCpuUtilizationPercentage: Int? = null,
  val targetMemoryUtilizationPercentage: Int? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("enabled", enabled)
      put("minReplicas", minReplicas)
      put("maxReplicas", maxReplicas)
      targetCpuUtilizationPercentage?.let { put("targetCpuUtilizationPercentage", it) }
      targetMemoryUtilizationPercentage?.let { put("targetMemoryUtilizationPercentage", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): AutoscalingSpec =
      AutoscalingSpec(
        enabled = json.getBoolean("enabled"),
        minReplicas = json.getInteger("minReplicas"),
        maxReplicas = json.getInteger("maxReplicas"),
        targetCpuUtilizationPercentage = json.getInteger("targetCpuUtilizationPercentage"),
        targetMemoryUtilizationPercentage = json.getInteger("targetMemoryUtilizationPercentage"),
      )
  }
}

/**
 * A Galley representation of a Kubernetes Pod spec
 */
data class ApplicationPodSpec(
  val containers: List<ContainerSpec>,
  val initContainers: List<ContainerSpec>? = null,
  val volumes: List<VolumeSpec>? = null,
  val serviceAccountName: String? = null,
  val restartPolicy: RestartPolicy? = RestartPolicy.Always,
  val terminationGracePeriodSeconds: Long? = null,
  val dnsPolicy: DnsPolicy? = null,
  val dnsConfig: DnsConfig? = null,
  val imagePullSecrets: List<String>? = null,
  val nodeSelector: Map<String, String>? = null,
  val nodeAffinity: JsonObject? = null,
  val podAffinity: JsonObject? = null,
  val podAntiAffinity: JsonObject? = null,
  val tolerations: List<Toleration>? = null,
  val topologySpreadConstraints: List<TopologySpreadConstraint>? = null,
  val securityContext: JsonObject? = null,
  val hostAliases: List<HostAlias>? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("containers", containers.map { it.toJson() })
      initContainers?.let { put("initContainers", it.map { c -> c.toJson() }) }
      volumes?.let { put("volumes", it.map { v -> v.toJson() }) }
      serviceAccountName?.let { put("serviceAccountName", it) }
      restartPolicy?.let { put("restartPolicy", it.name) }
      terminationGracePeriodSeconds?.let { put("terminationGracePeriodSeconds", it) }
      dnsPolicy?.let { put("dnsPolicy", it.name) }
      dnsConfig?.let { put("dnsConfig", it.toJson()) }
      imagePullSecrets?.let { put("imagePullSecrets", it) }
      nodeSelector?.let { put("nodeSelector", JsonObject(it)) }
      nodeAffinity?.let { put("nodeAffinity", it) }
      podAffinity?.let { put("podAffinity", it) }
      podAntiAffinity?.let { put("podAntiAffinity", it) }
      tolerations?.let { put("tolerations", it.map { t -> t.toJson() }) }
      topologySpreadConstraints?.let { put("topologySpreadConstraints", it.map { tsc -> tsc.toJson() }) }
      securityContext?.let { put("securityContext", it) }
      hostAliases?.let { put("hostAliases", it.map { ha -> ha.toJson() }) }
    }

  companion object {
    fun fromJson(json: JsonObject): ApplicationPodSpec =
      ApplicationPodSpec(
        containers = json.getJsonArray("containers").map { ContainerSpec.fromJson(it as JsonObject) },
        initContainers = json.getJsonArray("initContainers")?.map { ContainerSpec.fromJson(it as JsonObject) },
        volumes = json.getJsonArray("volumes")?.map { VolumeSpec.fromJson(it as JsonObject) },
        serviceAccountName = json.getString("serviceAccountName"),
        restartPolicy = json.getString("restartPolicy")?.let { RestartPolicy.valueOf(it) },
        terminationGracePeriodSeconds = json.getLong("terminationGracePeriodSeconds"),
        dnsPolicy = json.getString("dnsPolicy")?.let { DnsPolicy.valueOf(it) },
        dnsConfig = json.getJsonObject("dnsConfig")?.let { DnsConfig.fromJson(it) },
        imagePullSecrets = json.getJsonArray("imagePullSecrets")?.map { it as String },
        nodeSelector = json.getJsonObject("nodeSelector")?.map?.mapValues { it.value as String },
        nodeAffinity = json.getJsonObject("nodeAffinity"),
        podAffinity = json.getJsonObject("podAffinity"),
        podAntiAffinity = json.getJsonObject("podAntiAffinity"),
        tolerations = json.getJsonArray("tolerations")?.map { Toleration.fromJson(it as JsonObject) },
        topologySpreadConstraints =
          json
            .getJsonArray(
              "topologySpreadConstraints",
            )?.map { TopologySpreadConstraint.fromJson(it as JsonObject) },
        securityContext = json.getJsonObject("securityContext"),
        hostAliases = json.getJsonArray("hostAliases")?.map { HostAlias.fromJson(it as JsonObject) },
      )
  }
}

enum class RestartPolicy {
  Always,
  OnFailure,
  Never,
}

enum class DnsPolicy {
  ClusterFirst,
  ClusterFirstWithHostNet,
  Default,
  None,
}

data class DnsConfig(
  val nameservers: List<String>? = null,
  val searches: List<String>? = null,
  val options: List<DnsOption>? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      nameservers?.let { put("nameservers", it) }
      searches?.let { put("searches", it) }
      options?.let { put("options", it.map { o -> o.toJson() }) }
    }

  companion object {
    fun fromJson(json: JsonObject): DnsConfig =
      DnsConfig(
        nameservers = json.getJsonArray("nameservers")?.map { it as String },
        searches = json.getJsonArray("searches")?.map { it as String },
        options = json.getJsonArray("options")?.map { DnsOption.fromJson(it as JsonObject) },
      )
  }
}

data class DnsOption(
  val name: String,
  val value: String? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("name", name)
      value?.let { put("value", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): DnsOption =
      DnsOption(
        name = json.getString("name"),
        value = json.getString("value"),
      )
  }
}

data class Toleration(
  val key: String? = null,
  val operator: TolerationOperator? = null,
  val value: String? = null,
  val effect: TolerationEffect? = null,
  val tolerationSeconds: Long? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      key?.let { put("key", it) }
      operator?.let { put("operator", it.name) }
      value?.let { put("value", it) }
      effect?.let { put("effect", it.name) }
      tolerationSeconds?.let { put("tolerationSeconds", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): Toleration =
      Toleration(
        key = json.getString("key"),
        operator = json.getString("operator")?.let { TolerationOperator.valueOf(it) },
        value = json.getString("value"),
        effect = json.getString("effect")?.let { TolerationEffect.valueOf(it) },
        tolerationSeconds = json.getLong("tolerationSeconds"),
      )
  }
}

enum class TolerationOperator {
  Exists,
  Equal,
}

enum class TolerationEffect {
  NoSchedule,
  PreferNoSchedule,
  NoExecute,
}

data class TopologySpreadConstraint(
  val maxSkew: Int,
  val topologyKey: String,
  val whenUnsatisfiable: UnsatisfiableConstraintAction,
  val labelSelector: JsonObject? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("maxSkew", maxSkew)
      put("topologyKey", topologyKey)
      put("whenUnsatisfiable", whenUnsatisfiable.name)
      labelSelector?.let { put("labelSelector", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): TopologySpreadConstraint =
      TopologySpreadConstraint(
        maxSkew = json.getInteger("maxSkew"),
        topologyKey = json.getString("topologyKey"),
        whenUnsatisfiable = UnsatisfiableConstraintAction.valueOf(json.getString("whenUnsatisfiable")),
        labelSelector = json.getJsonObject("labelSelector"),
      )
  }
}

enum class UnsatisfiableConstraintAction {
  DoNotSchedule,
  ScheduleAnyway,
}

data class HostAlias(
  val ip: String,
  val hostnames: List<String>,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("ip", ip)
      put("hostnames", hostnames)
    }

  companion object {
    fun fromJson(json: JsonObject): HostAlias =
      HostAlias(
        ip = json.getString("ip"),
        hostnames = json.getJsonArray("hostnames").map { it as String },
      )
  }
}

/**
 * A Galley representation of a Kubernetes container
 */
data class ContainerSpec(
  val name: String,
  val image: String,
  val imageTag: String? = null,
  val imagePullPolicy: ImagePullPolicy? = null,
  val command: List<String>? = null,
  val args: List<String>? = null,
  val workingDir: String? = null,
  val ports: List<ContainerPort>? = null,
  val env: List<EnvVar>? = null,
  val envFrom: EnvFromSource? = null,
  val resources: ResourceRequirements? = null,
  val volumeMounts: List<VolumeMount>? = null,
  val livenessProbe: JsonObject? = null,
  val readinessProbe: JsonObject? = null,
  val startupProbe: JsonObject? = null,
  val lifecycle: JsonObject? = null,
  val securityContext: JsonObject? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("name", name)
      put("image", image)
      imageTag?.let { put("imageTag", it) }
      imagePullPolicy?.let { put("imagePullPolicy", it.name) }
      command?.let { put("command", it) }
      args?.let { put("args", it) }
      workingDir?.let { put("workingDir", it) }
      ports?.let { put("ports", it.map { p -> p.toJson() }) }
      env?.let { put("env", it.map { e -> e.toJson() }) }
      envFrom?.let { put("envFrom", it.toJson()) }
      resources?.let { put("resources", it.toJson()) }
      volumeMounts?.let { put("volumeMounts", it.map { vm -> vm.toJson() }) }
      livenessProbe?.let { put("livenessProbe", it) }
      readinessProbe?.let { put("readinessProbe", it) }
      startupProbe?.let { put("startupProbe", it) }
      lifecycle?.let { put("lifecycle", it) }
      securityContext?.let { put("securityContext", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): ContainerSpec =
      ContainerSpec(
        name = json.getString("name"),
        image = json.getString("image"),
        imageTag = json.getString("imageTag"),
        imagePullPolicy = json.getString("imagePullPolicy")?.let { ImagePullPolicy.valueOf(it) },
        command = json.getJsonArray("command")?.map { it as String },
        args = json.getJsonArray("args")?.map { it as String },
        workingDir = json.getString("workingDir"),
        ports = json.getJsonArray("ports")?.map { ContainerPort.fromJson(it as JsonObject) },
        env = json.getJsonArray("env")?.map { EnvVar.fromJson(it as JsonObject) },
        envFrom = json.getJsonObject("envFrom")?.let { EnvFromSource.fromJson(it) },
        resources = json.getJsonObject("resources")?.let { ResourceRequirements.fromJson(it) },
        volumeMounts = json.getJsonArray("volumeMounts")?.map { VolumeMount.fromJson(it as JsonObject) },
        livenessProbe = json.getJsonObject("livenessProbe"),
        readinessProbe = json.getJsonObject("readinessProbe"),
        startupProbe = json.getJsonObject("startupProbe"),
        lifecycle = json.getJsonObject("lifecycle"),
        securityContext = json.getJsonObject("securityContext"),
      )
  }
}

enum class ImagePullPolicy {
  Always,
  IfNotPresent,
  Never,
}

data class ContainerPort(
  val name: String? = null,
  val containerPort: Int,
  val protocol: Protocol? = Protocol.TCP,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      name?.let { put("name", it) }
      put("containerPort", containerPort)
      protocol?.let { put("protocol", it.name) }
    }

  companion object {
    fun fromJson(json: JsonObject): ContainerPort =
      ContainerPort(
        name = json.getString("name"),
        containerPort = json.getInteger("containerPort"),
        protocol = json.getString("protocol")?.let { Protocol.valueOf(it) },
      )
  }
}

enum class Protocol {
  TCP,
  UDP,
  SCTP,
}

data class EnvVar(
  val name: String,
  val value: String? = null,
  val valueFrom: EnvVarSource? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("name", name)
      value?.let { put("value", it) }
      valueFrom?.let { put("valueFrom", it.toJson()) }
    }

  companion object {
    fun fromJson(json: JsonObject): EnvVar =
      EnvVar(
        name = json.getString("name"),
        value = json.getString("value"),
        valueFrom = json.getJsonObject("valueFrom")?.let { EnvVarSource.fromJson(it) },
      )
  }
}

data class EnvVarSource(
  val configMapKeyRef: ConfigMapKeySelector? = null,
  val secretKeyRef: SecretKeySelector? = null,
  val fieldRef: JsonObject? = null,
  val resourceFieldRef: JsonObject? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      configMapKeyRef?.let { put("configMapKeyRef", it.toJson()) }
      secretKeyRef?.let { put("secretKeyRef", it.toJson()) }
      fieldRef?.let { put("fieldRef", it) }
      resourceFieldRef?.let { put("resourceFieldRef", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): EnvVarSource =
      EnvVarSource(
        configMapKeyRef = json.getJsonObject("configMapKeyRef")?.let { ConfigMapKeySelector.fromJson(it) },
        secretKeyRef = json.getJsonObject("secretKeyRef")?.let { SecretKeySelector.fromJson(it) },
        fieldRef = json.getJsonObject("fieldRef"),
        resourceFieldRef = json.getJsonObject("resourceFieldRef"),
      )
  }
}

data class ConfigMapKeySelector(
  val name: String? = null,
  val key: String,
  val optional: Boolean? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      name?.let { put("name", it) }
      put("key", key)
      optional?.let { put("optional", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): ConfigMapKeySelector =
      ConfigMapKeySelector(
        name = json.getString("name"),
        key = json.getString("key"),
        optional = json.getBoolean("optional"),
      )
  }
}

data class SecretKeySelector(
  val name: String? = null,
  val key: String,
  val optional: Boolean? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      name?.let { put("name", it) }
      put("key", key)
      optional?.let { put("optional", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): SecretKeySelector =
      SecretKeySelector(
        name = json.getString("name"),
        key = json.getString("key"),
        optional = json.getBoolean("optional"),
      )
  }
}

data class EnvFromSource(
  val configMaps: List<String>? = null,
  val secrets: List<String>? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      configMaps?.let { put("configMaps", it) }
      secrets?.let { put("secrets", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): EnvFromSource =
      EnvFromSource(
        configMaps = json.getJsonArray("configMaps")?.map { it as String },
        secrets = json.getJsonArray("secrets")?.map { it as String },
      )
  }
}

data class ResourceRequirements(
  val requests: ResourceList? = null,
  val limits: ResourceList? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      requests?.let { put("requests", it.toJson()) }
      limits?.let { put("limits", it.toJson()) }
    }

  companion object {
    fun fromJson(json: JsonObject): ResourceRequirements =
      ResourceRequirements(
        requests = json.getJsonObject("requests")?.let { ResourceList.fromJson(it) },
        limits = json.getJsonObject("limits")?.let { ResourceList.fromJson(it) },
      )
  }
}

data class ResourceList(
  val cpu: String? = null,
  val memory: String? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      cpu?.let { put("cpu", it) }
      memory?.let { put("memory", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): ResourceList =
      ResourceList(
        cpu = json.getString("cpu"),
        memory = json.getString("memory"),
      )
  }
}

data class VolumeMount(
  val name: String,
  val mountPath: String,
  val readOnly: Boolean? = null,
  val subPath: String? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("name", name)
      put("mountPath", mountPath)
      readOnly?.let { put("readOnly", it) }
      subPath?.let { put("subPath", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): VolumeMount =
      VolumeMount(
        name = json.getString("name"),
        mountPath = json.getString("mountPath"),
        readOnly = json.getBoolean("readOnly"),
        subPath = json.getString("subPath"),
      )
  }
}

/**
 * A Galley representation of a Kubernetes Volume
 */
data class VolumeSpec(
  val type: String,
  val name: String,
  val persistent: PersistentVolumeClaimVolumeSource? = null,
  val configMap: ConfigMapVolumeSource? = null,
  val secret: SecretVolumeSource? = null,
  val emptyDir: EmptyDirVolumeSource? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("type", type)
      put("name", name)
      persistent?.let { put("persistent", it.toJson()) }
      configMap?.let { put("configMap", it.toJson()) }
      secret?.let { put("secret", it.toJson()) }
      emptyDir?.let { put("emptyDir", it.toJson()) }
    }

  companion object {
    fun fromJson(json: JsonObject): VolumeSpec =
      VolumeSpec(
        type = json.getString("type"),
        name = json.getString("name"),
        persistent = json.getJsonObject("persistent")?.let { PersistentVolumeClaimVolumeSource.fromJson(it) },
        configMap = json.getJsonObject("configMap")?.let { ConfigMapVolumeSource.fromJson(it) },
        secret = json.getJsonObject("secret")?.let { SecretVolumeSource.fromJson(it) },
        emptyDir = json.getJsonObject("emptyDir")?.let { EmptyDirVolumeSource.fromJson(it) },
      )
  }
}

data class PersistentVolumeClaimVolumeSource(
  val claimName: String,
  val readOnly: Boolean? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("claimName", claimName)
      readOnly?.let { put("readOnly", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): PersistentVolumeClaimVolumeSource =
      PersistentVolumeClaimVolumeSource(
        claimName = json.getString("claimName"),
        readOnly = json.getBoolean("readOnly"),
      )
  }
}

data class ConfigMapVolumeSource(
  val configMapName: String,
  val defaultMode: Int? = null,
  val optional: Boolean? = null,
  val items: List<KeyToPath>? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("configMapName", configMapName)
      defaultMode?.let { put("defaultMode", it) }
      optional?.let { put("optional", it) }
      items?.let { put("items", it.map { i -> i.toJson() }) }
    }

  companion object {
    fun fromJson(json: JsonObject): ConfigMapVolumeSource =
      ConfigMapVolumeSource(
        configMapName = json.getString("configMapName"),
        defaultMode = json.getInteger("defaultMode"),
        optional = json.getBoolean("optional"),
        items = json.getJsonArray("items")?.map { KeyToPath.fromJson(it as JsonObject) },
      )
  }
}

data class SecretVolumeSource(
  val secretName: String,
  val defaultMode: Int? = null,
  val optional: Boolean? = null,
  val items: List<KeyToPath>? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("secretName", secretName)
      defaultMode?.let { put("defaultMode", it) }
      optional?.let { put("optional", it) }
      items?.let { put("items", it.map { i -> i.toJson() }) }
    }

  companion object {
    fun fromJson(json: JsonObject): SecretVolumeSource =
      SecretVolumeSource(
        secretName = json.getString("secretName"),
        defaultMode = json.getInteger("defaultMode"),
        optional = json.getBoolean("optional"),
        items = json.getJsonArray("items")?.map { KeyToPath.fromJson(it as JsonObject) },
      )
  }
}

data class KeyToPath(
  val key: String,
  val path: String,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      put("key", key)
      put("path", path)
    }

  companion object {
    fun fromJson(json: JsonObject): KeyToPath =
      KeyToPath(
        key = json.getString("key"),
        path = json.getString("path"),
      )
  }
}

data class EmptyDirVolumeSource(
  val medium: String? = "",
  val sizeLimit: String? = null,
) {
  fun toJson(): JsonObject =
    JsonObject().apply {
      medium?.let { put("medium", it) }
      sizeLimit?.let { put("sizeLimit", it) }
    }

  companion object {
    fun fromJson(json: JsonObject): EmptyDirVolumeSource =
      EmptyDirVolumeSource(
        medium = json.getString("medium"),
        sizeLimit = json.getString("sizeLimit"),
      )
  }
}
