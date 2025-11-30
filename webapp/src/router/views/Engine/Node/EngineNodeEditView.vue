<template>
  <form @submit.prevent="onSubmit" class="flex flex-col gap-8" ref="formRef" novalidate>
    <div class="card">
      <EngineNodeTabBar />

      <h1>Node Details & Setup</h1>
      <p class="mb-6">
        This configuration can be used to bind applications and/or databases to. You can change
        billing settings later, however once the configuration is set. You can’t edit it for
        applications deployed with this virtual server.
      </p>

      <h6>Configuration</h6>

      <div class="grid xl:grid-cols-4 gap-8">
        <UIFormField>
          <UILabel required for="name">Domain name</UILabel>
          <UITextInput required id="name" placeholder="e.g. app1.cloud.galley.run" v-model="name" />
          <label for="name" class="form-field__error-message">
            The domain name is a required field.
          </label>
          <label for="name">This name is used internally in Galley to identify this node.</label>
        </UIFormField>
        <UIFormField>
          <UILabel required for="ipAddress">IP Address</UILabel>
          <UITextInput
            required
            id="ipAddress"
            placeholder="e.g. 203.45.123.67"
            v-model="ipAddress"
          />
          <label for="ipAddress" class="form-field__error-message">
            The ip address should be a public accessible IPv4 address.
          </label>
        </UIFormField>
        <UIFormField>
          <div class="flex justify-between">
            <UILabel required for="region">Region</UILabel>
            <UIButton
              ghost
              small
              inline
              :trailing-addon="AddCircle"
              title="Add node"
              :onclick="() => (showRegionCreate = true)"
              >Add region</UIButton
            >
          </div>
          <UIDropDown select-first :items="regions" :disabled="isRegionsLoading" v-model="region">
            <template #leadingAddon="slotProps">
              <span v-if="slotProps?.item">
                <FlagIcon
                  :code="slotProps?.item?.metadata?.country"
                  v-if="slotProps?.item?.metadata?.country"
                />
              </span>
            </template>
            <template #trailingAddon="slotProps">
              <span v-if="slotProps?.item" class="dropdown__trailing-addon">{{
                slotProps?.item?.metadata?.providerName
              }}</span>
            </template>
          </UIDropDown>
          <label for="region" class="form-field__error-message">
            Choose the region where your node is running.
          </label>
        </UIFormField>
      </div>
      <div class="grid xl:grid-cols-2 gap-8">
        <UIFormField :aria-disabled="engineNode?.attributes?.provisioningStatus === 'ready'">
          <UILabel required for="nodeType">Node type</UILabel>
          <div>
            <UIRadioButton
              required
              id="nodeType"
              :disabled="engineNode?.attributes?.provisioningStatus === 'ready'"
              value="controller"
              v-model="nodeType"
              label="Controller"
              description="A controller manages the cluster."
            />
            <UIRadioButton
              required
              id="nodeType"
              :disabled="engineNode?.attributes?.provisioningStatus === 'ready'"
              value="controller-worker"
              v-model="nodeType"
              label="Controller & Worker"
              description="Choose this node type to run a single-node k0s cluster."
            />
            <UIRadioButton
              required
              id="nodeType"
              :disabled="engineNode?.attributes?.provisioningStatus === 'ready' || isFirstNode"
              value="worker"
              v-model="nodeType"
              label="Worker"
              description="Workers run your applications."
            />
          </div>
        </UIFormField>
        <UIFormField :aria-disabled="nodeType === 'controller'">
          <UILabel required for="deploy">Deploy to this node</UILabel>
          <div>
            <UIRadioButton
              required
              :disabled="nodeType === 'controller'"
              id="deploy"
              value="both"
              v-model="deploy"
              label="Applications & Databases"
              description="Allow both apps and databases on this node."
            />
            <UIRadioButton
              required
              :disabled="nodeType === 'controller'"
              id="deploy"
              value="applications"
              v-model="deploy"
              label="Applications"
              description="Make this node run isolated stateless applications and services."
            />
            <UIRadioButton
              required
              :disabled="nodeType === 'controller'"
              id="deploy"
              value="databases"
              v-model="deploy"
              label="Databases"
              description="Make this node dedicated for high IOPS and memory intensive workloads."
            />
          </div>
          <label>
            Separate apps and databases to improve performance, reliability, and security, or run
            both on the same node for simplicity in small setups.
          </label>
        </UIFormField>
      </div>

      <SlashesDivider
        class="opacity-30 mb-6"
        v-if="featureProvisioningEnabled || (engineNode && !isLoading)"
      />

      <h6 v-if="featureProvisioningEnabled">Provisioning</h6>

      <div class="grid xl:grid-cols-4 gap-8" v-if="featureProvisioningEnabled">
        <UIFormField>
          <UILabel for="provisioning">Automatic Provisioning</UILabel>
          <UIToggle
            id="name"
            :disabled="!name || !ipAddress"
            label="Let Galley to automatically manage and provision this node"
            v-model="provisioning"
          />
          <label for="provisioning" v-if="!name || !ipAddress"
            >A valid domain name and IP address are required to enable automatic
            provisioning.</label
          >
          <label for="provisioning" v-else>
            A freshly installed server (or VPS) is necessary. We recommend to use Ubuntu Server LTS
            as OS.
          </label>
        </UIFormField>
        <UIFormField v-if="provisioning">
          <UILabel for="sshKey">SSH Key</UILabel>
          <UIDropDown
            id="sshKey"
            v-model="sshKey"
            :items="[
              /** Add other ssh keys here */
              { label: 'Create new SSH key', value: 'create' },
            ]"
            placeholder="Select or create SSH key"
          >
            <template #buttonLeadingAddon>
              <Key class="shrink-0" />
            </template>
          </UIDropDown>
        </UIFormField>
      </div>

      <UICodeBlock
        title="SSH into your machine and run:"
        v-if="engineNode && engineNode.attributes.token && !isLoading"
      >
        <UICodeLine comment
          >All executions below need to be run via sudo to make server-wide changes.</UICodeLine
        >
        <UICodeLine empty />
        <UICodeLine v-if="engineNode.attributes.nodeType === 'worker'" comment
          >First, run this on your controller node:</UICodeLine
        >
        <UICodeLine v-if="engineNode.attributes.nodeType === 'worker'"
          >sudo galley worker invite</UICodeLine
        >
        <UICodeLine empty />
        <UICodeLine v-if="engineNode.attributes.nodeType === 'worker'" comment
          >Run the rest of the commands on your worker node:</UICodeLine
        >
        <UICodeLine>curl -sSf {{ getUrl }} | sudo sh</UICodeLine>
        <UICodeLine empty />
        <UICodeLine comment
          >Prepares your node with latest updates and secures your machine</UICodeLine
        >
        <UICodeLine> sudo galley node prepare </UICodeLine>
        <UICodeLine empty />
        <UICodeLine v-if="engineNode.attributes.nodeType === 'controller'" comment
          >Mark this node as controller in your cluster</UICodeLine
        >
        <UICodeLine v-if="engineNode.attributes.nodeType === 'controller'"
          >sudo galley controller join {{ engineNode.attributes.token }}</UICodeLine
        >
        <UICodeLine v-if="engineNode.attributes.nodeType === 'controller+worker'" comment
          >Mark this node as single-node cluster</UICodeLine
        >
        <UICodeLine v-if="engineNode.attributes.nodeType === 'controller+worker'"
          >sudo galley controller join {{ engineNode.attributes.token }} --single</UICodeLine
        >
        <UICodeLine v-if="engineNode.attributes.nodeType === 'worker'" comment
          >Mark this node as worker in your cluster</UICodeLine
        >
        <UICodeLine v-if="engineNode.attributes.nodeType === 'worker'"
          >sudo galley worker join</UICodeLine
        >
      </UICodeBlock>

      TODO: If status is imported and deployMode and region will be set, set the status to ready.

      <div class="card__footer form-footer">
        <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" to="/vessel/engine"
          >Back to engine
        </UIButton>
        <UIButton :disabled="isPending" ghost variant="destructive" v-if="nodeId"
          >Delete this node
        </UIButton>
        <UIButton :disabled="isPending" type="submit" v-if="nodeId"
          >Save this node
          <LoadingIndicator v-if="isPending" />
        </UIButton>
        <UIButton :disabled="isPending" type="submit" v-else :trailing-addon="DoubleAltArrowRight"
          >Start node provisioning
          <LoadingIndicator v-if="isPending" />
        </UIButton>
      </div>
    </div>
  </form>

  <RegionCreateDrawer
    :show="showRegionCreate"
    @close="() => (showRegionCreate = false)"
    @select="selectRegion"
  />
</template>
<script setup lang="ts">
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import UIFormField from '@/components/FormField/UIFormField.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import axios from 'axios'
import { useProjectsStore } from '@/stores/projects.ts'
import { storeToRefs } from 'pinia'
import FlagIcon from 'vue3-flag-icons'
import type { ApiResponse } from '@/types/api'
import type { EngineNodeSummary, EngineRegionSummary } from '@/types/api/engine'
import UIButton from '@/components/UIButton.vue'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import { AddCircle, ArrowLeft, DoubleAltArrowRight, Key } from '@solar-icons/vue'
import UIRadioButton from '@/components/FormField/UIRadioButton.vue'
import SlashesDivider from '@/assets/SlashesDivider.vue'
import UIToggle from '@/components/FormField/UIToggle.vue'
import UICodeBlock from '@/components/CodeBlock/UICodeBlock.vue'
import UICodeLine from '@/components/CodeBlock/UICodeLine.vue'
import { useRoute } from 'vue-router'
import EngineNodeTabBar from '@/router/views/Engine/Node/EngineNodeTabBar.vue'
import RegionCreateDrawer from '@/components/Drawer/RegionCreateDrawer.vue'

const projectsStore = useProjectsStore()
const { selectedVesselId } = storeToRefs(projectsStore)

const formRef = ref<HTMLFormElement | null>(null)

const route = useRoute()
const { nodeId } = route.params

const getUrl = import.meta.env.VITE_GET_URL

const {
  isLoading: isRegionsLoading,
  data: engineRegions,
  refetch: refetchRegions,
} = useQuery({
  enabled: !!selectedVesselId?.value,
  queryKey: ['vessel', selectedVesselId?.value, 'engine', 'regions'],
  queryFn: () =>
    axios.get<ApiResponse<EngineRegionSummary>[], ApiResponse<EngineRegionSummary>[]>(
      `/vessels/${selectedVesselId?.value}/engine/regions`,
    ),
})

const {
  isLoading,
  data: engineNode,
  refetch: refetchNode,
} = useQuery({
  enabled: !!selectedVesselId?.value && !!nodeId,
  queryKey: ['vessel', selectedVesselId?.value, 'engine', 'nodes', nodeId],
  queryFn: () =>
    axios.get<ApiResponse<EngineNodeSummary>, ApiResponse<EngineNodeSummary>>(
      `/vessels/${selectedVesselId?.value}/engine/nodes/${nodeId}`,
    ),
})

// useMutation({
//   mutationFn: (nodeId, data) => {
//     if (!nodeId) {
//       return axios.post(`/vessels/${selectedVesselId?.value}/engine/nodes`, data)
//     }
//
//     return axios.patch(`/vessels/${selectedVesselId?.value}/engine/nodes/${nodeId}`, data)
//   },
// })

const regions = computed(() => {
  const items = engineRegions.value?.map((region: ApiResponse<EngineRegionSummary>) => ({
    label: region.attributes.name,
    value: region.id,
    metadata: {
      providerName: region.attributes.providerName,
      country: region.attributes.locationCountry,
    },
  }))

  if (!items || items.length === 0) {
    return [{ label: 'No regions registered.', disabled: true }]
  }

  return items
})

const showRegionCreate = ref(false)
const name = ref('')
const ipAddress = ref('')
const nodeType = ref('controller')
const deploy = ref('')
const sshKey = ref('')
const region = ref('')
const provisioning = ref(true)

let refreshNodeTimeout: NodeJS.Timeout
watch(
  engineNode,
  (value) => {
    if (value) {
      name.value = value.attributes.name
      ipAddress.value = value.attributes.ipAddress
      nodeType.value = value.attributes.nodeType
      deploy.value = value.attributes.deployMode
      provisioning.value = value.attributes.provisioning
      region.value = value.attributes.vesselEngineRegionId

      if (refreshNodeTimeout) {
        clearTimeout(refreshNodeTimeout)
      }

      if (value.attributes.provisioningStatus === 'open') {
        refreshNodeTimeout = setTimeout(async () => {
          await refetchNode()
        }, 30000)
      }
    }
  },
  { immediate: true },
)

const { data: engineNodes } = useQuery({
  enabled: computed(() => !!selectedVesselId?.value),
  queryKey: computed(() => ['vessel', selectedVesselId?.value, 'engine', 'nodes']),
  queryFn: () =>
    axios.get<ApiResponse<EngineNodeSummary>[], ApiResponse<EngineNodeSummary>[]>(
      `/vessels/${selectedVesselId?.value}/engine/nodes`,
    ),
})

onBeforeUnmount(() => {
  if (refreshNodeTimeout) {
    clearTimeout(refreshNodeTimeout)
  }
})

const featureProvisioningEnabled = ref(false)

watch(nodeType, (value) => {
  if (value === 'controller') {
    deploy.value = ''
  }
})

watch(sshKey, (value) => {
  if (value === 'create') {
    // start loader, create new ssh key in the background, reload keys and select the newly created one
    alert('Create SSH key')
  }
})

async function selectRegion(regionId: string) {
  await refetchRegions()
  region.value = regionId
}

const isFirstNode = computed(
  () =>
    engineNodes?.value?.length === 1 &&
    engineNodes?.value?.[0]?.attributes.provisioningStatus === 'ready',
)

const isPending = ref(false)

async function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  // if (first) controller and provisionig? set engine to managed, if controller and not provisioning set engine to controlled
}
</script>
