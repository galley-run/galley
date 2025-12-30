import { useCharterResource } from './useCharterResource'
import type {
  OAuthConnection,
  CreateOAuthConnectionRequest,
} from '@/types/oauth'

/**
 * Composable for managing OAuth connections at the charter level
 */
export const useCharterConnection = () => {
  return useCharterResource<
    OAuthConnection,
    CreateOAuthConnectionRequest,
    Partial<CreateOAuthConnectionRequest>
  >('connections', 'connectionId')
}

// Export individual hooks for convenience
export const {
  useResources: useConnections,
  useResource: useConnection,
  useCreateResource: useCreateConnection,
  useUpdateResource: useUpdateConnection,
  useDeleteResource: useDeleteConnection,
  useSaveResource: useSaveConnection,
} = useCharterConnection()
