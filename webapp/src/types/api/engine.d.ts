export interface EngineSummary {
  name: string
  mode: string
}

export interface EngineNodeSummary {
  name: string
  ipAddress: string
  nodeType: string
  deployMode: string
  cpu: string
  memory: string
  storage: string
  vesselEngineRegionId: string
  provisioning: boolean
  osMetadata: object
  provisioningStatus: string
}

export interface EngineRegionSummary {
  name: string
  providerName: string
  geoRegion: string
  locationCity: string
  locationCountry: string
}
