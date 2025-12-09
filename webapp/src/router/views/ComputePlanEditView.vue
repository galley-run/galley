<template>
  <form @submit.prevent="onSubmit" class="flex flex-col gap-8" ref="formRef" novalidate>
    <div class="card">
      <h1 v-if="computePlanId">Edit compute plan</h1>
      <h1 v-else>Add new compute plan</h1>
      <p class="mb-6">
        This configuration can be used to bind applications and/or databases to. You can change
        billing settings later, however once the configuration is set, you can't edit it for
        applications deployed with this compute plan.
      </p>

      <div class="space-y-8">
        <div class="space-y-4">
          <h6>Compute plan details</h6>

          <div class="grid grid-cols-1 xl:grid-cols-4 gap-8">
            <UIFormField>
              <UILabel for="name" required>Name</UILabel>
              <UITextInput
                required
                name="name"
                id="name"
                placeholder="e.g. Compute Small"
                v-model="name"
              />
              <label for="name" class="form-field__error-message">
                The name is a required field.
              </label>
            </UIFormField>

            <UIFormField>
              <UILabel required for="application">Use for</UILabel>
              <UIDropDown
                required
                name="application"
                placeholder="Compute plan may apply for..."
                id="application"
                v-model="application"
                :items="applicationTypes"
              />
              <label for="application">
                Specify what type of workloads can use this compute plan.
              </label>
            </UIFormField>
          </div>
        </div>

        <div class="space-y-4">
          <div class="flex items-center justify-between">
            <h6>Resource size</h6>
          </div>
          <p>Guaranteed resources for applications and databases using this plan.</p>

          <div class="grid xl:grid-cols-4 gap-8">
            <UIFormField>
              <UILabel for="requestsCpu" required @info-click="toggleResourceChefRecommendations()"
                >CPU</UILabel
              >
              <component
                :is="advancedMode ? UITextInput : UIDropDown"
                required
                name="requestsCpu"
                id="requestsCpu"
                :items="cpuAutoComplete"
                placeholder="e.g. 1"
                v-model="requestsCpu"
                trailing-addon="vCPU"
              />
              <label for="requestsCpu">Number of CPU cores (e.g., 1, 2, 0.5)</label>
            </UIFormField>
            <UIFormField>
              <UILabel
                for="requestsMemory"
                required
                @info-click="toggleResourceChefRecommendations()"
                >Memory</UILabel
              >
              <component
                :is="advancedMode ? UITextInput : UIDropDown"
                required
                name="requestsMemory"
                :items="memoryAutoComplete"
                id="requestsMemory"
                placeholder="e.g. 512Mi or 1Gi"
                v-model="requestsMemory"
              />
              <label for="requestsMemory">Memory amount (e.g., 512Mi, 1Gi, 2Gi)</label>
            </UIFormField>
            <UIFormField>
              <UILabel for="advancedMode">Advanced settings</UILabel>
              <UIToggle label="Allow precision values" v-model="advancedMode" />
              <UIToggle label="Enable burstable instance" v-model="burstMode" />
              <label for="billingUnitPrice"
                >Enabling burstable instance will allow you to define resource limits.</label
              >
            </UIFormField>
          </div>
        </div>

        <div class="space-y-4" v-if="burstMode">
          <h6>Resource limits</h6>
          <p>
            You can set resource limits, which your
            {{ getApplicationType(application).toLowerCase() }} can consume.<br />
            Resource limits can never be lower than the regular resource size.
          </p>

          <div class="grid xl:grid-cols-4 gap-8">
            <UIFormField>
              <UILabel
                for="limitsCpu"
                required
                @info-click="toggleResourceLimitsChefRecommendations()"
                >CPU limit</UILabel
              >
              <component
                :is="advancedMode ? UITextInput : UIDropDown"
                required
                id="limitsCpu"
                name="limitsCpu"
                :min="requestsCpu"
                :items="cpuAutoComplete"
                :placeholder="advancedMode ? 'e.g. 1' : 'e.g. 1 vCPU'"
                v-model="limitsCpu"
                trailing-addon="vCPU"
              />
              <label for="limitsCpu">Maximum CPU cores</label>
            </UIFormField>
            <UIFormField>
              <UILabel
                for="limitsMemory"
                required
                @info-click="toggleResourceLimitsChefRecommendations()"
                >Memory limit</UILabel
              >
              <component
                :is="advancedMode ? UITextInput : UIDropDown"
                required
                name="limitsMemory"
                :min="requestsMemory"
                :items="memoryAutoComplete"
                id="limitsMemory"
                placeholder="e.g. 512M or 1G"
                v-model="limitsMemory"
              />
              <label for="limitsMemory">Maximum memory amount</label>
            </UIFormField>
          </div>
        </div>

        <!--        TODO: Enable billing section once we have a proper billing solution and can handle multi currency .-->
        <!--        <SlashesDivider class="opacity-30" />-->

        <!--        <div class="space-y-4">-->
        <!--          <h6>Billing</h6>-->

        <!--          <div class="grid xl:grid-cols-4 gap-8">-->
        <!--            <UIFormField>-->
        <!--              <UIToggle-->
        <!--                id="billingEnabled"-->
        <!--                label="Enable billing for this compute plan"-->
        <!--                name="billingEnabled"-->
        <!--                v-model="billingEnabled"-->
        <!--              />-->
        <!--            </UIFormField>-->

        <!--            <UIFormField v-if="billingEnabled">-->
        <!--              <UILabel for="billingPeriod">Billing period</UILabel>-->
        <!--              <UIDropDown-->
        <!--                id="billingPeriod"-->
        <!--                name="billingPeriod"-->
        <!--                v-model="billingPeriod"-->
        <!--                :items="[{ value: 'monthly', label: 'Monthly' }]"-->
        <!--              />-->
        <!--            </UIFormField>-->

        <!--            <UIFormField v-if="billingEnabled">-->
        <!--              <UILabel for="billingUnitPrice">Unit price</UILabel>-->
        <!--              <UITextInput-->
        <!--                id="billingUnitPrice"-->
        <!--                name="billingUnitPrice"-->
        <!--                leading-addon="&euro;"-->
        <!--                format="money"-->
        <!--                placeholder="e.g. 5.50"-->
        <!--                v-model="billingUnitPrice"-->
        <!--              />-->
        <!--              <label for="billingUnitPrice">Price per billing period</label>-->
        <!--            </UIFormField>-->
        <!--          </div>-->
        <!--        </div>-->

        <div v-if="error" class="alert alert--destructive flex items-center">
          <Danger />
          <div class="alert__body">
            {{ error }}
          </div>
        </div>

        <div class="card__footer form-footer">
          <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" to="/charter/compute-plan">
            Back
          </UIButton>
          <UIButton
            :disabled="saveIsPending || deleteIsPending"
            ghost
            variant="destructive"
            v-if="computePlanId"
            @click="confirmDelete = true"
          >
            Delete this plan
            <LoadingIndicator v-if="deleteIsPending" />
          </UIButton>
          <UIButton :disabled="saveIsPending" type="submit" v-if="computePlanId">
            Save this plan
            <LoadingIndicator v-if="saveIsPending" />
          </UIButton>
          <UIButton :disabled="saveIsPending" type="submit" v-else :trailing-addon="AddCircle">
            Create plan
            <LoadingIndicator v-if="saveIsPending" />
          </UIButton>
        </div>
      </div>
    </div>
  </form>

  <ConfirmDeleteComputePlanDialog
    :show="confirmDelete"
    @close="confirmDelete = false"
    @confirm="onDelete"
    v-if="!!computePlanId"
    :compute-plan-id="computePlanId"
  />
  <ResourceRecommendationsChefDialog
    :show="showResourceChefRecommendations"
    @close="() => (showResourceChefRecommendations = false)"
  />
  <ResourceLimitsRecommendationsChefDialog
    :show="showResourceLimitsChefRecommendations"
    @close="() => (showResourceLimitsChefRecommendations = false)"
  />
</template>

<script setup lang="ts">
import UILabel from '@/components/FormField/UILabel.vue'
import UIFormField from '@/components/FormField/UIFormField.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import { computed, ref, watch } from 'vue'
import UIButton from '@/components/UIButton.vue'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import { AddCircle, ArrowLeft, Danger } from '@solar-icons/vue'
import { useRoute, useRouter } from 'vue-router'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import UIToggle from '@/components/FormField/UIToggle.vue'
import {
  applicationTypes,
  getApplicationType,
  useComputePlanFormHelpers,
  useDeleteComputePlan,
  useSaveComputePlan,
} from '@/composables/useComputePlan.ts'
import ConfirmDeleteComputePlanDialog from '@/components/Dialog/ConfirmDeleteComputePlanDialog.vue'
import type { ApiError } from '@/utils/registerAxios.ts'
import ResourceRecommendationsChefDialog from '@/components/Dialog/ResourceRecommendationsChefDialog.vue'
import ResourceLimitsRecommendationsChefDialog from '@/components/Dialog/ResourceLimitsRecommendationsChefDialog.vue'

const formRef = ref<HTMLFormElement | null>(null)
const confirmDelete = ref(false)
const error = ref<string | null>(null)
const advancedMode = ref(false)
const showResourceChefRecommendations = ref(false)
const showResourceLimitsChefRecommendations = ref(false)

const route = useRoute()
const router = useRouter()
const computePlanId = computed(() => route.params.computePlanId as string | undefined)
const charterId = computed(() => route.params.charterId as string | undefined)
const vesselId = computed(() => route.params.vesselId as string | undefined)

const {
  name,
  application,
  requestsCpu,
  requestsMemory,
  limitsCpu,
  limitsMemory,
  burstMode,
  saveComputePlan,
} = useComputePlanFormHelpers(computePlanId, charterId, vesselId)

watch(
  burstMode,
  (value) => {
    if (!value) {
      limitsCpu.value = undefined
      limitsMemory.value = undefined
    }
  },
  { immediate: true },
)

const { isPending: saveIsPending } = useSaveComputePlan(computePlanId, charterId, vesselId)
const { isPending: deleteIsPending } = useDeleteComputePlan(charterId, vesselId)

const cpuAutoComplete = [
  { value: '0.1', label: '0.1 vCPU' },
  { value: '0.25', label: '0.25 vCPU' },
  { value: '0.5', label: '0.5 vCPU' },
  { value: '1', label: '1 vCPU' },
  { value: '2', label: '2 vCPU' },
  { value: '4', label: '4 vCPU' },
  { value: '8', label: '8 vCPU' },
  { value: '16', label: '16 vCPU' },
]

const memoryAutoComplete = [
  { value: '128Mi', label: '128 MiB' },
  { value: '256Mi', label: '256 MiB' },
  { value: '512Mi', label: '512 MiB' },
  { value: '1Gi', label: '1 GiB' },
  { value: '2Gi', label: '2 GiB' },
  { value: '4Gi', label: '4 GiB' },
  { value: '8Gi', label: '8 GiB' },
  { value: '16Gi', label: '16 GiB' },
  { value: '32Gi', label: '32 GiB' },
]

async function onSubmit() {
  error.value = null
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity()
    return
  }

  try {
    await saveComputePlan()
    await router.push('/charter/compute-plan')
  } catch (e) {
    const apiError = e as ApiError
    error.value = apiError?.message || 'Something went wrong. Please try again later.'
  }
}

async function onDelete() {
  await router.push('/charter/compute-plan')
}

function toggleResourceChefRecommendations() {
  showResourceChefRecommendations.value = !showResourceChefRecommendations.value
}
function toggleResourceLimitsChefRecommendations() {
  showResourceLimitsChefRecommendations.value = !showResourceLimitsChefRecommendations.value
}
</script>
