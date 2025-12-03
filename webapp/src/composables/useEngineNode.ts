import { type MaybeRefOrGetter, ref, watch } from 'vue'
import type { EngineNodeSummary } from '@/types/api/engine'
import { useVesselResource } from './useVesselResource'

export const nodeTypes = [
  { value: 'controller', label: 'Controller' },
  { value: 'worker', label: 'Worker' },
  { value: 'controller_worker', label: 'Controller + Worker' },
]

export const deployModes = [
  { value: 'applications', label: 'Applications' },
  { value: 'databases', label: 'Databases' },
  { value: 'applications_databases', label: 'Applications & Databases' },
]

export const provisioningStatuses = [
  { value: 'open', label: 'Open' },
  { value: 'ready', label: 'Ready' },
  { value: 'imported', label: 'Imported' },
]

// Node-specific types
export type NodeCreateData = {
  id: string
}

export type NodeUpdateData = {
  ipAddress?: string
  name?: string
  nodeType?: 'controller' | 'worker' | 'controller_worker'
  vesselEngineRegionId?: string | null
  provisioning?: boolean
  deployMode?: 'applications' | 'databases' | 'applications_databases' | null
  scheduledUpdates?: any[]
}

// Create resource-specific composables
const nodeResource = useVesselResource<EngineNodeSummary, NodeCreateData, NodeUpdateData>(
  'engine/nodes',
  'nodeId',
)

// Export with node-specific names
export function useNodes(vesselId?: MaybeRefOrGetter<string | undefined>) {
  const { resources, ...rest } = nodeResource.useResources(vesselId)
  return { nodes: resources, ...rest }
}

export function useNode(
  nodeId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { resource, ...rest } = nodeResource.useResource(nodeId, vesselId)
  return { node: resource, ...rest }
}

export function useCreateNode(vesselId?: MaybeRefOrGetter<string | undefined>) {
  const { createResource, ...rest } = nodeResource.useCreateResource(vesselId)
  return { createNode: createResource, ...rest }
}

export function useUpdateNode(
  nodeId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { updateResource, ...rest } = nodeResource.useUpdateResource(nodeId, vesselId)
  return { updateNode: updateResource, ...rest }
}

export function useDeleteNode(vesselId?: MaybeRefOrGetter<string | undefined>) {
  const { deleteResource, ...rest } = nodeResource.useDeleteResource(vesselId)
  return { deleteNode: deleteResource, ...rest }
}

export function useSaveNode(
  nodeId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { saveResource, ...rest } = nodeResource.useSaveResource(nodeId, vesselId)
  return { saveNode: saveResource, ...rest }
}

// Form helpers with watchers for create/edit forms
export function useNodeFormHelpers(vesselId?: MaybeRefOrGetter<string | undefined>) {
  const name = ref('')
  const ipAddress = ref('')
  const nodeType = ref<'controller' | 'worker' | 'controller_worker'>('worker')
  const vesselEngineRegionId = ref<string | null | undefined>(null)
  const provisioning = ref(true)
  const deployMode = ref<'applications' | 'databases' | 'applications_databases' | undefined>(
    undefined,
  )

  const { saveNode } = useSaveNode()

  const { node } = useNode(undefined, vesselId)

  // Load existing node data into form
  watch(
    node,
    (nodeData) => {
      if (nodeData) {
        name.value = nodeData.attributes.name || ''
        ipAddress.value = nodeData.attributes.ipAddress || ''
        nodeType.value = nodeData.attributes.nodeType
        vesselEngineRegionId.value = nodeData.attributes.vesselEngineRegionId
        provisioning.value = nodeData.attributes.provisioning
        deployMode.value = nodeData.attributes.deployMode || 'applications_databases'
      }
    },
    { immediate: true },
  )

  const pristine = ref(true)
  watch([name, ipAddress, nodeType, vesselEngineRegionId, provisioning, deployMode], (values) => {
    pristine.value = values.filter(Boolean).length === 0
  })

  return {
    saveNode: () =>
      saveNode({
        name: name.value,
        ipAddress: ipAddress.value,
        nodeType: nodeType.value,
        vesselEngineRegionId: vesselEngineRegionId.value,
        provisioning: provisioning.value,
        deployMode: deployMode.value ?? null,
      }),
    name,
    ipAddress,
    nodeType,
    vesselEngineRegionId,
    provisioning,
    deployMode,
    pristine,
  }
}
