import { computed, type MaybeRefOrGetter, toValue } from 'vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import axios, { type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types/api'
import { useRoute } from 'vue-router'
import { useProjectsStore } from '@/stores/projects.ts'
import { storeToRefs } from 'pinia'

// Helper to get vessel ID from route or store
function useVesselId(vesselId?: MaybeRefOrGetter<string | undefined>) {
  const route = useRoute()
  const projectsStore = useProjectsStore()
  const { selectedVesselId } = storeToRefs(projectsStore)

  return computed(() => {
    if (vesselId !== undefined) {
      const value = toValue(vesselId)
      if (value) return value
    }
    return (route.params.vesselId as string) || selectedVesselId.value
  })
}

// Helper to get resource ID from route
function useResourceId(
  resourceIdParam: string,
  resourceId?: MaybeRefOrGetter<string | undefined>,
) {
  const route = useRoute()

  return computed(() => {
    if (resourceId !== undefined) {
      const value = toValue(resourceId)
      if (value) return value
    }
    return route.params[resourceIdParam] as string
  })
}

/**
 * Generic composable for vessel resources (regions, nodes, etc)
 * @param resourcePath - The resource path (e.g., 'engine/regions', 'engine/nodes')
 * @param resourceIdParam - The route param name for the resource ID (e.g., 'regionId', 'nodeId')
 */
export function useVesselResource<TResource, TCreateData, TUpdateData = Partial<TCreateData>>(
  resourcePath: string,
  resourceIdParam: string,
) {
  // Get all resources for a vessel
  function useResources(vesselId?: MaybeRefOrGetter<string | undefined>) {
    const vesselIdValue = useVesselId(vesselId)

    const query = useQuery({
      enabled: computed(() => !!vesselIdValue.value),
      queryKey: computed(() => ['vessel', vesselIdValue.value, ...resourcePath.split('/')]),
      queryFn: () =>
        axios.get<ApiResponse<TResource>[], ApiResponse<TResource>[]>(
          `/vessels/${vesselIdValue.value}/${resourcePath}`,
        ),
    })

    return {
      resources: query.data,
      isLoading: query.isLoading,
      error: query.error,
      refetch: query.refetch,
    }
  }

  // Get a single resource
  function useResource(
    resourceId?: MaybeRefOrGetter<string | undefined>,
    vesselId?: MaybeRefOrGetter<string | undefined>,
  ) {
    const vesselIdValue = useVesselId(vesselId)
    const resourceIdValue = useResourceId(resourceIdParam, resourceId)

    const query = useQuery({
      enabled: computed(() => !!vesselIdValue.value && !!resourceIdValue.value),
      queryKey: computed(() => [
        'vessel',
        vesselIdValue.value,
        ...resourcePath.split('/'),
        resourceIdValue.value,
      ]),
      queryFn: () =>
        axios.get<ApiResponse<TResource>, ApiResponse<TResource>>(
          `/vessels/${vesselIdValue.value}/${resourcePath}/${resourceIdValue.value}`,
        ),
    })

    return {
      resource: query.data,
      isLoading: query.isLoading,
      error: query.error,
      refetch: query.refetch,
    }
  }

  // Create a resource
  function useCreateResource(vesselId?: MaybeRefOrGetter<string | undefined>) {
    const queryClient = useQueryClient()
    const vesselIdValue = useVesselId(vesselId)

    const mutation = useMutation({
      mutationFn: (data: TCreateData) =>
        axios.post<ApiResponse<TResource>, ApiResponse<TResource>>(
          `/vessels/${vesselIdValue.value}/${resourcePath}`,
          data,
        ),
      onSuccess: async () => {
        await queryClient.invalidateQueries({
          queryKey: ['vessel', vesselIdValue.value, ...resourcePath.split('/')],
        })
      },
    })

    return {
      createResource: mutation.mutateAsync,
      isPending: mutation.isPending,
      error: mutation.error,
    }
  }

  // Update a resource
  function useUpdateResource(
    resourceId?: MaybeRefOrGetter<string | undefined>,
    vesselId?: MaybeRefOrGetter<string | undefined>,
  ) {
    const queryClient = useQueryClient()
    const vesselIdValue = useVesselId(vesselId)
    const resourceIdValue = useResourceId(resourceIdParam, resourceId)

    const mutation = useMutation({
      mutationFn: (data: TUpdateData) =>
        axios.patch<ApiResponse<TResource>, ApiResponse<TResource>>(
          `/vessels/${vesselIdValue.value}/${resourcePath}/${resourceIdValue.value}`,
          data,
        ),
      onSuccess: async (updatedResource) => {
        const singleResourceKey = [
          'vessel',
          vesselIdValue.value,
          ...resourcePath.split('/'),
          resourceIdValue.value,
        ]
        const listResourceKey = ['vessel', vesselIdValue.value, ...resourcePath.split('/')]

        // Update single resource cache with response data
        queryClient.setQueryData(singleResourceKey, updatedResource)

        // Update the resource in the list cache
        queryClient.setQueryData(listResourceKey, (oldData: ApiResponse<TResource>[] | undefined) => {
          if (!oldData) return oldData
          return oldData.map((item) =>
            item.id === updatedResource.id ? updatedResource : item,
          )
        })
      },
    })

    return {
      updateResource: mutation.mutateAsync,
      isPending: mutation.isPending,
      error: mutation.error,
    }
  }

  // Delete a resource
  function useDeleteResource(vesselId?: MaybeRefOrGetter<string | undefined>) {
    const queryClient = useQueryClient()
    const vesselIdValue = useVesselId(vesselId)

    const mutation = useMutation({
      mutationFn: (resourceId: string) =>
        axios.delete(`/vessels/${vesselIdValue.value}/${resourcePath}/${resourceId}`),
      onSuccess: () => {
        queryClient.invalidateQueries({
          queryKey: ['vessel', vesselIdValue.value, ...resourcePath.split('/')],
        })
      },
    })

    return {
      deleteResource: mutation.mutateAsync,
      isPending: mutation.isPending,
      error: mutation.error,
    }
  }

  // Save resource (auto-detect create vs update based on resourceId)
  function useSaveResource(
    resourceId?: MaybeRefOrGetter<string | undefined>,
    vesselId?: MaybeRefOrGetter<string | undefined>,
  ) {
    const queryClient = useQueryClient()
    const vesselIdValue = useVesselId(vesselId)
    const resourceIdValue = useResourceId(resourceIdParam, resourceId)

    const mutation = useMutation({
      mutationFn: (data: TCreateData | TUpdateData) => {
        if (resourceIdValue.value) {
          // Update existing resource
          return axios.patch<ApiResponse<TResource>, ApiResponse<TResource>>(
            `/vessels/${vesselIdValue.value}/${resourcePath}/${resourceIdValue.value}`,
            data,
          )
        } else {
          // Create new resource
          return axios.post<ApiResponse<TResource>, ApiResponse<TResource>>(
            `/vessels/${vesselIdValue.value}/${resourcePath}`,
            data,
          )
        }
      },
      onSuccess: async (savedResource) => {
        const listResourceKey = ['vessel', vesselIdValue.value, ...resourcePath.split('/')]

        if (resourceIdValue.value) {
          // Update: set both single resource and list cache
          const singleResourceKey = [
            'vessel',
            vesselIdValue.value,
            ...resourcePath.split('/'),
            resourceIdValue.value,
          ]

          queryClient.setQueryData(singleResourceKey, savedResource)

          queryClient.setQueryData(
            listResourceKey,
            (oldData: ApiResponse<TResource>[] | undefined) => {
              if (!oldData) return oldData
              return oldData.map((item) =>
                item.id === savedResource.id ? savedResource : item,
              )
            },
          )
        } else {
          // Create: invalidate list to refetch with new item
          await queryClient.invalidateQueries({
            queryKey: listResourceKey,
          })
        }
      },
    })

    return {
      saveResource: mutation.mutateAsync,
      isPending: mutation.isPending,
      error: mutation.error,
      isCreating: computed(() => !resourceIdValue.value),
      isUpdating: computed(() => !!resourceIdValue.value),
    }
  }

  return {
    useResources,
    useResource,
    useCreateResource,
    useUpdateResource,
    useDeleteResource,
    useSaveResource,
  }
}
