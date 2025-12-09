import { type MaybeRefOrGetter, ref, watch } from 'vue'
import type { ComputePlan } from '@/types/api/computePlan'
import { useCharterResource } from './useCharterResource'

// Compute plan-specific types based on OpenAPI spec
export type ComputePlanCreateData = {
  name: string
  application: 'applications' | 'databases' | 'applications_databases' | null
  requests: {
    cpu: string
    memory: string
  }
  limits?: {
    cpu: string
    memory: string
  } | null
  billing?: {
    enabled: boolean
    period?: 'monthly'
    unitPrice?: string
  }
}

export type ComputePlanUpdateData = {
  name?: string
  application?: 'applications' | 'databases' | 'applications_databases' | null
  requests?: {
    cpu: string
    memory: string
  }
  limits?: {
    cpu: string
    memory: string
  } | null
  billing?: {
    enabled: boolean
    period?: 'monthly'
    unitPrice?: string
  }
}

// Create resource-specific composables
const computePlanResource = useCharterResource<
  ComputePlan,
  ComputePlanCreateData,
  ComputePlanUpdateData
>('compute-plans', 'computePlanId')

// Export with compute plan-specific names
export function useComputePlans(
  charterId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { resources, ...rest } = computePlanResource.useResources(charterId, vesselId)
  return { computePlans: resources, ...rest }
}

export function useComputePlan(
  computePlanId?: MaybeRefOrGetter<string | undefined>,
  charterId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { resource, ...rest } = computePlanResource.useResource(
    computePlanId,
    charterId,
    vesselId,
  )
  return { computePlan: resource, ...rest }
}

export function useCreateComputePlan(
  charterId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { createResource, ...rest } = computePlanResource.useCreateResource(charterId, vesselId)
  return { createComputePlan: createResource, ...rest }
}

export function useUpdateComputePlan(
  computePlanId?: MaybeRefOrGetter<string | undefined>,
  charterId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { updateResource, ...rest } = computePlanResource.useUpdateResource(
    computePlanId,
    charterId,
    vesselId,
  )
  return { updateComputePlan: updateResource, ...rest }
}

export function useDeleteComputePlan(
  charterId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { deleteResource, ...rest } = computePlanResource.useDeleteResource(charterId, vesselId)
  return { deleteComputePlan: deleteResource, ...rest }
}

export function useSaveComputePlan(
  computePlanId?: MaybeRefOrGetter<string | undefined>,
  charterId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { saveResource, ...rest } = computePlanResource.useSaveResource(
    computePlanId,
    charterId,
    vesselId,
  )
  return { saveComputePlan: saveResource, ...rest }
}

// Application types for dropdown
export const applicationTypes = [
  { value: 'applications_databases', label: 'Applications & Databases' },
  { value: 'applications', label: 'Applications' },
  { value: 'databases', label: 'Databases' },
]

export function getApplicationType(application: string | null) {
  return applicationTypes.find((type) => type.value === application)?.label || 'Applications & Databases'
}

// Form helpers with watchers for create/edit forms
export function useComputePlanFormHelpers(
  computePlanId?: MaybeRefOrGetter<string | undefined>,
  charterId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const name = ref('')
  const application = ref<'applications' | 'databases' | 'applications_databases' | null>(
    'applications_databases',
  )
  const requestsCpu = ref('0.25')
  const requestsMemory = ref('128Mi')
  const limitsCpu = ref<string | undefined>('0.25')
  const limitsMemory = ref<string | undefined>('128Mi')
  const billingEnabled = ref(false)
  const billingPeriod = ref<'monthly'>('monthly')
  const billingUnitPrice = ref<string | undefined>(undefined)
  const burstMode = ref(false)

  const { saveComputePlan } = useSaveComputePlan(computePlanId, charterId, vesselId)
  const { computePlan } = useComputePlan(computePlanId, charterId, vesselId)

  // Load existing compute plan data into form
  watch(
    computePlan,
    (data) => {
      if (data) {
        name.value = data.attributes.name || ''
        application.value = data.attributes.application
        requestsCpu.value = data.attributes.requests.cpu
        requestsMemory.value = data.attributes.requests.memory
        limitsCpu.value = data.attributes.limits?.cpu
        limitsMemory.value = data.attributes.limits?.memory
        billingEnabled.value = data.attributes.billing?.enabled || false
        billingPeriod.value = data.attributes.billing?.period || 'monthly'
        billingUnitPrice.value = data.attributes.billing?.unitPrice

        burstMode.value = !(data.attributes.requests.cpu === data.attributes?.limits?.cpu && data.attributes.requests.memory === data.attributes?.limits?.memory)
      }
    },
    { immediate: true },
  )

  return {
    saveComputePlan: () =>
      saveComputePlan({
        name: name.value,
        application: application.value,
        requests: {
          cpu: requestsCpu.value,
          memory: requestsMemory.value,
        },
        limits:
          limitsCpu.value && limitsMemory.value
            ? {
                cpu: limitsCpu.value,
                memory: limitsMemory.value,
              }
            : null,
        billing: billingEnabled.value
          ? {
              enabled: billingEnabled.value,
              period: billingPeriod.value,
              unitPrice: billingUnitPrice.value,
            }
          : undefined,
      }),
    name,
    application,
    requestsCpu,
    requestsMemory,
    limitsCpu,
    limitsMemory,
    billingEnabled,
    billingPeriod,
    billingUnitPrice,
    burstMode,
  }
}
