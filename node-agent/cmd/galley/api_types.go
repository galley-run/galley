package main

type VesselEngineNodeAttributes struct {
	VesselEngineRegionID string `json:"vesselEngineRegionId"`
	NodeType             string `json:"nodeType"`
	VesselEngineID       string `json:"vesselEngineId"`
	Name                 string `json:"name"`
	IPAddress            string `json:"ipAddress"`
	CPU                  string `json:"cpu"`
	Memory               string `json:"memory"`
	Storage              string `json:"storage"`
	Provisioning         bool   `json:"provisioning"`
}

type DataResource[T any] struct {
	ID         string `json:"id"`
	Type       string `json:"type"`
	Attributes T      `json:"attributes"`
}

type VesselEngineNodeResponse struct {
	Data DataResource[VesselEngineNodeAttributes] `json:"data"`
}
