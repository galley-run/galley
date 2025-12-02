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
              title="Add new region"
              :onclick="() => (showRegionCreateDrawer = true)"
              >Add region</UIButton
            >
          </div>
          <UIDropDown
            select-first
            required
            :items="regionItems"
            :disabled="isRegionsLoading"
            v-model="vesselEngineRegionId"
          >
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
        <UIFormField :aria-disabled="node?.attributes?.provisioningStatus === 'ready'">
          <UILabel required for="nodeType">Node type</UILabel>
          <div>
            <UIRadioButton
              required
              name="nodeType"
              id="nodeType-controller"
              :disabled="node?.attributes?.provisioningStatus === 'ready'"
              value="controller"
              v-model="nodeType"
              label="Controller"
              description="A controller manages the cluster."
            />
            <UIRadioButton
              required
              name="nodeType"
              id="nodeType-controller"
              :disabled="node?.attributes?.provisioningStatus === 'ready'"
              value="controller_worker"
              v-model="nodeType"
              label="Controller & Worker"
              description="Choose this node type to run a single-node k0s cluster."
            />
            <UIRadioButton
              required
              name="nodeType"
              id="nodeType-worker"
              :disabled="node?.attributes?.provisioningStatus === 'ready' || isFirstNode"
              value="worker"
              v-model="nodeType"
              label="Worker"
              description="Workers run your applications."
            />
          </div>
          <label for="nodeType" class="form-field__error-message">
            Please choose a node type.
          </label>
        </UIFormField>
        <UIFormField :aria-disabled="nodeType === 'controller'">
          <UILabel required for="deployMode">Deploy to this node</UILabel>
          <div>
            <UIRadioButton
              required
              :disabled="nodeType === 'controller'"
              id="deployMode-applications_databases"
              name="deployMode"
              value="applications_databases"
              v-model="deployMode"
              label="Applications & Databases"
              description="Allow both apps and databases on this node."
            />
            <UIRadioButton
              required
              :disabled="nodeType === 'controller'"
              id="deployMode-applications"
              name="deployMode"
              value="applications"
              v-model="deployMode"
              label="Applications"
              description="Make this node run isolated stateless applications and services."
            />
            <UIRadioButton
              required
              :disabled="nodeType === 'controller'"
              id="deployMode-databases"
              name="deployMode"
              value="databases"
              v-model="deployMode"
              label="Databases"
              description="Make this node dedicated for high IOPS and memory intensive workloads."
            />
          </div>
          <label>
            In bigger clusters, separation of apps and databases will improve performance,
            reliability, and security.
            <span v-if="!nodes || nodes?.length < 3"
              >However for development purpose and in smaller setups running both on the same node
              can win in simplicity in smaller setups.</span
            >
          </label>
          <label for="deployMode" class="form-field__error-message">
            Choose what you want to deploy to this node.
          </label>
        </UIFormField>
      </div>

      <SlashesDivider
        class="opacity-30 mb-6"
        v-if="featureProvisioningEnabled || (node && !isLoading)"
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
        <!--        <UIFormField v-if="provisioning">-->
        <!--          <UILabel for="sshKey">SSH Key</UILabel>-->
        <!--          <UIDropDown-->
        <!--            id="sshKey"-->
        <!--            v-model="sshKey"-->
        <!--            :items="[-->
        <!--              /** Add other ssh keys here */-->
        <!--              { label: 'Create new SSH key', value: 'create' },-->
        <!--            ]"-->
        <!--            placeholder="Select or create SSH key"-->
        <!--          >-->
        <!--            <template #buttonLeadingAddon>-->
        <!--              <Key class="shrink-0" />-->
        <!--            </template>-->
        <!--          </UIDropDown>-->
        <!--        </UIFormField>-->
      </div>

      <UICodeBlock
        title="SSH into your machine and run:"
        v-if="node && node.attributes.token && !isLoading"
      >
        <UICodeLine comment
          >All executions below need to be run via sudo to make server-wide changes.</UICodeLine
        >
        <UICodeLine empty />
        <UICodeLine v-if="node.attributes.nodeType === 'worker'" comment
          >First, run this on your controller node:</UICodeLine
        >
        <UICodeLine v-if="node.attributes.nodeType === 'worker'"
          >sudo galley worker invite</UICodeLine
        >
        <UICodeLine empty />
        <UICodeLine v-if="node.attributes.nodeType === 'worker'" comment
          >Run the rest of the commands on your worker node:</UICodeLine
        >
        <UICodeLine>curl -sSf {{ getUrl }} | sudo sh</UICodeLine>
        <UICodeLine empty />
        <UICodeLine comment
          >Prepares your node with latest updates and secures your machine</UICodeLine
        >
        <UICodeLine> sudo galley node prepare </UICodeLine>
        <UICodeLine empty />
        <UICodeLine v-if="node.attributes.nodeType === 'controller'" comment
          >Mark this node as controller in your cluster</UICodeLine
        >
        <UICodeLine v-if="node.attributes.nodeType === 'controller'"
          >sudo galley controller join {{ node.attributes.token }}</UICodeLine
        >
        <UICodeLine v-if="node.attributes.nodeType === 'controller_worker'" comment
          >Mark this node as single-node cluster</UICodeLine
        >
        <UICodeLine v-if="node.attributes.nodeType === 'controller_worker'"
          >sudo galley controller join {{ node.attributes.token }} --single</UICodeLine
        >
        <UICodeLine v-if="node.attributes.nodeType === 'worker'" comment
          >Mark this node as worker in your cluster</UICodeLine
        >
        <UICodeLine v-if="node.attributes.nodeType === 'worker'"
          >sudo galley worker join</UICodeLine
        >
      </UICodeBlock>

      TODO: If status is imported and deployMode and region will be set, set the status to ready.

      <div v-if="error" class="alert alert--destructive">
        <Danger />

        {{ error }}
      </div>
      <div class="card__footer form-footer">
        <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" to="/vessel/engine"
          >Back to engine
        </UIButton>
        <UIButton
          v-if="nodeId"
          :disabled="isPending || node?.attributes?.provisioningStatus !== 'open'"
          ghost
          variant="destructive"
          @click="confirmDelete = true"
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

  <footer v-if="nodeId && node?.attributes?.provisioningStatus !== 'open'">
    <h5>Why can't this node be deleted?</h5>
    <p>Nodes can only be removed while they're being provisioned.</p>
  </footer>

  <RegionCreateDrawer
    :show="showRegionCreateDrawer"
    @close="() => (showRegionCreateDrawer = false)"
    @select="selectRegion"
  />
  <ConfirmDeleteNodeDialog
    :show="confirmDelete"
    @close="confirmDelete = false"
    @confirm="onDeleteNode"
    v-if="nodeId"
    :node-id="nodeId"
  />
</template>
<script setup lang="ts">
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import UIFormField from '@/components/FormField/UIFormField.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import { computed, ref, watch } from 'vue'
import FlagIcon from 'vue3-flag-icons'
import UIButton from '@/components/UIButton.vue'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import { AddCircle, ArrowLeft, DoubleAltArrowRight, Danger } from '@solar-icons/vue'
import UIRadioButton from '@/components/FormField/UIRadioButton.vue'
import SlashesDivider from '@/assets/SlashesDivider.vue'
import UIToggle from '@/components/FormField/UIToggle.vue'
import UICodeBlock from '@/components/CodeBlock/UICodeBlock.vue'
import UICodeLine from '@/components/CodeBlock/UICodeLine.vue'
import { useRoute } from 'vue-router'
import EngineNodeTabBar from '@/router/views/Engine/Node/EngineNodeTabBar.vue'
import RegionCreateDrawer from '@/components/Drawer/RegionCreateDrawer.vue'
import { useNode, useNodeFormHelpers, useNodes, useSaveNode } from '@/composables/useEngineNode.ts'
import { useRegions } from '@/composables/useEngineRegion.ts'
import router from '@/router'
import ConfirmDeleteNodeDialog from '@/components/Dialog/ConfirmDeleteNodeDialog.vue'
import type { ApiError } from '@/utils/registerAxios.ts'

const formRef = ref<HTMLFormElement | null>(null)
const confirmDelete = ref<boolean>(false)

const route = useRoute()
const nodeId = computed(() => route.params.nodeId as string | undefined)
const vesselId = computed(() => route.params.vesselId as string | undefined)

const getUrl = import.meta.env.VITE_GET_URL

// const {
//   isLoading: isRegionsLoading,
//   data: engineRegions,
//   refetch: refetchRegions,
// } = useQuery({
//   enabled: !!vesselId,
//   queryKey: ['vessel', vesselId, 'engine', 'regions'],
//   queryFn: () =>
//     axios.get<ApiResponse<EngineRegionSummary>[], ApiResponse<EngineRegionSummary>[]>(
//       `/vessels/${vesselId}/engine/regions`,
//     ),
// })

const { node, isLoading } = useNode()

// useMutation({
//   mutationFn: (nodeId, data) => {
//     if (!nodeId) {
//       return axios.post(`/vessels/${vesselId}/engine/nodes`, data)
//     }
//
//     return axios.patch(`/vessels/${vesselId}/engine/nodes/${nodeId}`, data)
//   },
// })

const regionItems = computed(() => {
  return (
    regions.value?.map((region) => ({
      label: region.attributes.name,
      value: region.id,
      metadata: {
        providerName: region.attributes.providerName,
        country: region.attributes.locationCountry,
      },
    })) ?? [{ label: 'Add your first region', disabled: true }]
  )
})

const showRegionCreateDrawer = ref(false)
const error = ref<string | null>(null)

const { name, ipAddress, nodeType, deployMode, vesselEngineRegionId, provisioning, saveNode } =
  useNodeFormHelpers()
const { nodes } = useNodes()
const { isPending } = useSaveNode()
const { regions, isLoading: isRegionsLoading, refetch: refetchRegions } = useRegions()

// let refreshNodeTimeout: NodeJS.Timeout
// watch(
//   node,
//   (value) => {
//     if (value) {
//       name.value = value.attributes.name
//       ipAddress.value = value.attributes.ipAddress
//       nodeType.value = value.attributes.nodeType
//       deployMode.value = value.attributes.deployMode
//       provisioning.value = value.attributes.provisioning
//       region.value = value.attributes.vesselEngineRegionId ?? ''
//
//       if (refreshNodeTimeout) {
//         clearTimeout(refreshNodeTimeout)
//       }
//
//       if (value.attributes.provisioningStatus === 'open') {
//         refreshNodeTimeout = setTimeout(async () => {
//           await refetchNode()
//         }, 30000)
//       }
//     }
//   },
//   { immediate: true },
// )
//

// onBeforeUnmount(() => {
//   if (refreshNodeTimeout) {
//     clearTimeout(refreshNodeTimeout)
//   }
// })

const featureProvisioningEnabled = ref(false)

watch(nodeType, (value) => {
  if (value === 'controller') {
    deployMode.value = ''
  }
})

async function selectRegion(regionId: string) {
  await refetchRegions()
  vesselEngineRegionId.value = regionId
}

const isFirstNode = computed(() => nodes?.value?.length === 0)

async function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  try {
  const node = await saveNode()
  if (nodeId.value) {
    await router.push('/vessel/engine')
  } else {
    await router.push(`/vessel/${vesselId.value}/engine/node/${node.id}`)
  }
  } catch (e) {
      const apiError = e as ApiError
      error.value = apiError?.message || 'Something went wrong. Please try again later.'
    }
  // if (first) controller and provisionig? set engine to managed, if controller and not provisioning set engine to controlled
}

async function onDeleteNode() {
  await router.push('/vessel/engine')
}
</script>
