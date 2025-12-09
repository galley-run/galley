export interface ComputePlan {
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
