<template>
  <form @submit.prevent="onSubmit" class="flex flex-col gap-8" ref="formRef" novalidate>
    <div class="card">
      <h1>Node Details & Setup</h1>
      <p class="mb-6">
        This configuration can be used to bind applications and/or databases to. You can change
        billing settings later, however once the configuration is set. You can’t edit it for
        applications deployed with this virtual server.
      </p>

      <h6>Configuration</h6>

      <div class="grid xl:grid-cols-4 gap-8">
        <UIFormField>
          <UILabel required for="domainName">Domain name</UILabel>
          <UITextInput
            required
            id="domainName"
            placeholder="e.g. app1.cloud.galley.run"
            v-model="domainName"
          />
          <label for="domainName" class="form-field__error-message">
            The domain name is a required field.
          </label>
          <label for="domainName"
            >This name is used internally in Galley to identify this node.</label
          >
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
          <UILabel required for="region">Region</UILabel>
          <UIDropDown select-first :items="regions" :disabled="isRegionsLoading" v-model="region">
            <template #leadingAddon="slotProps">
              <span v-if="slotProps?.item">
                <FlagIcon :code="slotProps?.item?.metadata?.country" />
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
        <UIFormField>
          <UILabel required for="nodeType">Node type</UILabel>
          <div>
            <UIRadioButton
              required
              id="nodeType"
              value="controller"
              v-model="nodeType"
              label="Controller"
              description="A controller manages the cluster."
            />
            <UIRadioButton
              required
              id="nodeType"
              value="controller-worker"
              v-model="nodeType"
              label="Controller & Worker"
              description="Choose this node type to run a single-node k0s cluster."
            />
            <UIRadioButton
              required
              id="nodeType"
              value="worker"
              v-model="nodeType"
              :disabled="isFirstNode"
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
          <label
            >Separate apps and databases to improve performance, reliability, and security, or run
            both on the same node for simplicity in small setups.</label
          >
        </UIFormField>
      </div>

      <SlashesDivider class="opacity-30 mb-6" />

      <h6 v-if="featureProvisioningEnabled">Provisioning</h6>

      <div class="grid xl:grid-cols-4 gap-8" v-if="featureProvisioningEnabled">
        <UIFormField>
          <UILabel for="provisioning">Automatic Provisioning</UILabel>
          <UIToggle
            id="domainName"
            :disabled="!domainName || !ipAddress"
            label="Let Galley to automatically manage and provision this node"
            v-model="provisioning"
          />
          <label for="provisioning" v-if="!domainName || !ipAddress"
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

      <UICodeBlock title="Run this on your node">
        <UICodeLine comment>Install Galley Node Agent (as root)</UICodeLine>
        <UICodeLine>curl -sSf https://galley.run/install.sh | sh</UICodeLine>
        <UICodeLine empty />
        <UICodeLine>galley connect --node “03260D35-C0C0-4AA9-9F72-1620B02CC9D5” --pubkey “ssh-ed...ACTUAL PUB KEY...” --platform-url “cloud.galley.run”</UICodeLine>
      </UICodeBlock>

      <div class="card__footer form-footer">
        <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" to="/vessel/engine"
          >Back to engine
        </UIButton>
        <UIButton :disabled="isPending" ghost variant="destructive"
          >Delete this node
        </UIButton>
        <UIButton :disabled="isPending" type="submit"
          >Add this node to your engine
          <LoadingIndicator v-if="isPending" />
        </UIButton>
      </div>
    </div>
  </form>
</template>
<script setup lang="ts">
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import UIFormField from '@/components/FormField/UIFormField.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import { computed, ref, watch } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import axios from 'axios'
import { useProjectsStore } from '@/stores/projects.ts'
import { storeToRefs } from 'pinia'
import FlagIcon from 'vue3-flag-icons'
import type { ApiResponse } from '@/types/api'
import type { EngineRegionSummary } from '@/types/api/engine'
import UIButton from '@/components/UIButton.vue'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import { ArrowLeft, Key } from '@solar-icons/vue'
import UIRadioButton from '@/components/FormField/UIRadioButton.vue'
import SlashesDivider from '@/assets/SlashesDivider.vue'
import UIToggle from '@/components/FormField/UIToggle.vue'
import UICodeBlock from '@/components/CodeBlock/UICodeBlock.vue'
import UICodeLine from '@/components/CodeBlock/UICodeLine.vue'

const projectsStore = useProjectsStore()
const { selectedVesselId } = storeToRefs(projectsStore)

const formRef = ref<HTMLFormElement | null>(null)

const { isLoading: isRegionsLoading, data: engineRegions } = useQuery({
  enabled: !!selectedVesselId?.value,
  queryKey: ['vessel', selectedVesselId?.value, 'engine', 'regions'],
  queryFn: () =>
    axios.get(`/vessels/${selectedVesselId?.value}/engine/regions`) as Promise<
      ApiResponse<EngineRegionSummary>[]
    >,
})

const regions = computed(
  () =>
    engineRegions.value?.map((region) => ({
      label: region.attributes.name,
      value: region.id,
      metadata: {
        providerName: region.attributes.providerName,
        country: region.attributes.locationCountry,
      },
    })) ?? [],
)

const domainName = ref('app.cloud.run')
const ipAddress = ref('4')
const nodeType = ref('controller')
const deploy = ref('')
const sshKey = ref('')
const region = computed(() => regions.value?.[0]?.value)
const provisioning = ref(true)

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

const isFirstNode = ref(true)

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
