import { computed, type MaybeRefOrGetter } from 'vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import axios from 'axios'
import type { ApiResponse } from '@/types/api'
import { useVesselId, useCharterId, useResourceId } from './useResourceHelpers'

/**
 * Generic composable for charter resources (compute-plans, etc)
 * @param resourcePath - The resource path (e.g., 'compute-plans')
 * @param resourceIdParam - The route param name for the resource ID (e.g., 'computePlanId')
 */
export function useCharterResource<TResource, TCreateData, TUpdateData = Partial<TCreateData>>(
  resourcePath: string,
  resourceIdParam: string,
) {
  // Get all resources for a charter
  function useResources(
    charterId?: MaybeRefOrGetter<string | undefined>,
    vesselId?: MaybeRefOrGetter<string | undefined>,
  ) {
    const vesselIdValue = useVesselId(vesselId)
    const charterIdValue = useCharterId(charterId)

    const query = useQuery({
      enabled: computed(() => !!vesselIdValue.value && !!charterIdValue.value),
      queryKey: computed(() => [
        'vessel',
        vesselIdValue.value,
        'charter',
        charterIdValue.value,
        ...resourcePath.split('/'),
      ]),
      queryFn: () =>
        axios.get<ApiResponse<TResource>[], ApiResponse<TResource>[]>(
          `/vessels/${vesselIdValue.value}/charters/${charterIdValue.value}/${resourcePath}`,
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
    charterId?: MaybeRefOrGetter<string | undefined>,
    vesselId?: MaybeRefOrGetter<string | undefined>,
  ) {
    const vesselIdValue = useVesselId(vesselId)
    const charterIdValue = useCharterId(charterId)
    const resourceIdValue = useResourceId(resourceIdParam, resourceId)

    const query = useQuery({
      enabled: computed(
        () => !!vesselIdValue.value && !!charterIdValue.value && !!resourceIdValue.value,
      ),
      queryKey: computed(() => [
        'vessel',
        vesselIdValue.value,
        'charter',
        charterIdValue.value,
        ...resourcePath.split('/'),
        resourceIdValue.value,
      ]),
      queryFn: () =>
        axios.get<ApiResponse<TResource>, ApiResponse<TResource>>(
          `/vessels/${vesselIdValue.value}/charters/${charterIdValue.value}/${resourcePath}/${resourceIdValue.value}`,
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
  function useCreateResource(
    charterId?: MaybeRefOrGetter<string | undefined>,
    vesselId?: MaybeRefOrGetter<string | undefined>,
  ) {
    const queryClient = useQueryClient()
    const vesselIdValue = useVesselId(vesselId)
    const charterIdValue = useCharterId(charterId)

    const mutation = useMutation({
      mutationFn: (data: TCreateData) =>
        axios.post<ApiResponse<TResource>, ApiResponse<TResource>>(
          `/vessels/${vesselIdValue.value}/charters/${charterIdValue.value}/${resourcePath}`,
          data,
        ),
      onSuccess: async () => {
        await queryClient.invalidateQueries({
          queryKey: [
            'vessel',
            vesselIdValue.value,
            'charter',
            charterIdValue.value,
            ...resourcePath.split('/'),
          ],
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
    charterId?: MaybeRefOrGetter<string | undefined>,
    vesselId?: MaybeRefOrGetter<string | undefined>,
  ) {
    const queryClient = useQueryClient()
    const vesselIdValue = useVesselId(vesselId)
    const charterIdValue = useCharterId(charterId)
    const resourceIdValue = useResourceId(resourceIdParam, resourceId)

    const mutation = useMutation({
      mutationFn: (data: TUpdateData) =>
        axios.patch<ApiResponse<TResource>, ApiResponse<TResource>>(
          `/vessels/${vesselIdValue.value}/charters/${charterIdValue.value}/${resourcePath}/${resourceIdValue.value}`,
          data,
        ),
      onSuccess: async (updatedResource) => {
        const singleResourceKey = [
          'vessel',
          vesselIdValue.value,
          'charter',
          charterIdValue.value,
          ...resourcePath.split('/'),
          resourceIdValue.value,
        ]
        const listResourceKey = [
          'vessel',
          vesselIdValue.value,
          'charter',
          charterIdValue.value,
          ...resourcePath.split('/'),
        ]

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
  function useDeleteResource(
    charterId?: MaybeRefOrGetter<string | undefined>,
    vesselId?: MaybeRefOrGetter<string | undefined>,
  ) {
    const queryClient = useQueryClient()
    const vesselIdValue = useVesselId(vesselId)
    const charterIdValue = useCharterId(charterId)

    const mutation = useMutation({
      mutationFn: (resourceId: string) =>
        axios.delete(
          `/vessels/${vesselIdValue.value}/charters/${charterIdValue.value}/${resourcePath}/${resourceId}`,
        ),
      onSuccess: () => {
        queryClient.invalidateQueries({
          queryKey: [
            'vessel',
            vesselIdValue.value,
            'charter',
            charterIdValue.value,
            ...resourcePath.split('/'),
          ],
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
    charterId?: MaybeRefOrGetter<string | undefined>,
    vesselId?: MaybeRefOrGetter<string | undefined>,
  ) {
    const queryClient = useQueryClient()
    const vesselIdValue = useVesselId(vesselId)
    const charterIdValue = useCharterId(charterId)
    const resourceIdValue = useResourceId(resourceIdParam, resourceId)

    const mutation = useMutation({
      mutationFn: (data: TCreateData | TUpdateData) => {
        if (resourceIdValue.value) {
          // Update existing resource
          return axios.patch<ApiResponse<TResource>, ApiResponse<TResource>>(
            `/vessels/${vesselIdValue.value}/charters/${charterIdValue.value}/${resourcePath}/${resourceIdValue.value}`,
            data,
          )
        } else {
          // Create new resource
          return axios.post<ApiResponse<TResource>, ApiResponse<TResource>>(
            `/vessels/${vesselIdValue.value}/charters/${charterIdValue.value}/${resourcePath}`,
            data,
          )
        }
      },
      onSuccess: async (savedResource) => {
        const listResourceKey = [
          'vessel',
          vesselIdValue.value,
          'charter',
          charterIdValue.value,
          ...resourcePath.split('/'),
        ]

        if (resourceIdValue.value) {
          // Update: set both single resource and list cache
          const singleResourceKey = [
            'vessel',
            vesselIdValue.value,
            'charter',
            charterIdValue.value,
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
