export type OAuthConnectionType = 'git' | 'registry'

export type OAuthProvider = 'github' | 'gitlab' | 'bitbucket' | 'dockerhub' | 'ghcr'

export type OAuthConnectionStatus = 'pending' | 'active' | 'revoked' | 'error'

export type OAuthCredentialKind = 'oauth_token' | 'github_app_installation' | 'pat'

export type OAuthGrantPrincipalType = 'user' | 'team' | 'role'

export type OAuthGrantPermission = 'use' | 'manage' | 'revoke'

export interface OAuthCredential {
  id: string
  credential_kind: OAuthCredentialKind
  expires_at?: string | null
  installation_id?: number | null
  app_id?: number | null
  account_login?: string | null
  account_type?: string | null
  instance_url?: string | null
}

export interface OAuthConnectionGrant {
  id: string
  principal_type: OAuthGrantPrincipalType
  principal_id: string
  permission: OAuthGrantPermission
  created_at: string
}

export interface OAuthConnection {
  id: string
  vessel_id?: string | null
  charter_id?: string | null
  type: OAuthConnectionType
  provider: OAuthProvider
  status: OAuthConnectionStatus
  display_name: string
  created_by_user_id: string
  provider_account_id?: string | null
  scopes?: string[] | null
  last_validated_at?: string | null
  created_at: string
  updated_at: string
  credential: OAuthCredential
  grants: OAuthConnectionGrant[]
}

export interface CreateOAuthConnectionRequest {
  type: OAuthConnectionType
  provider: OAuthProvider
  display_name: string
  credential: {
    credential_kind: OAuthCredentialKind
    access_token?: string
    refresh_token?: string
    expires_at?: string
    token_type?: string
    installation_id?: number
    app_id?: number
    account_login?: string
    account_type?: string
    instance_url?: string
  }
  provider_account_id?: string
  scopes?: string[]
  default_grants?: Array<{
    principal_type: OAuthGrantPrincipalType
    principal_id: string
    permission: OAuthGrantPermission
  }>
}
