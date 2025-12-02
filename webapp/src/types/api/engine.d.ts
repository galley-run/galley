export interface EngineSummary {
  name: string
  mode: string
  agentConnectionStatus: string
  lastAgentConnectionAt: string
}

export interface EngineNodeSummary {
  name: string
  ipAddress: string
  nodeType: 'controller' | 'worker' | 'controller_worker'
  deployMode: 'applications' | 'databases' | 'applications_databases'
  cpu?: string
  memory?: string
  storage?: string
  vesselEngineRegionId?: string
  provisioning: boolean
  osMetadata: EngineNodeOSMetadata
  provisioningStatus: string
  token?: string
}

export interface EngineNodeOSMetadata {
    os?: string
    arch?: string
    distro?: string
    cpuUsed?: string
    version?: string
    memoryUsed?: string
    storageUsed?: string
}

export interface EngineRegionSummary {
  name: string
  providerName: string
  geoRegion: string
  locationCity: string
  locationCountry: string
}
