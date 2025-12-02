export default function(nodeType: 'worker' | 'controller' | 'controller_worker' | string) {
  switch (nodeType) {
    case 'controller_worker': return 'Controller & Worker'
    case 'controller': return 'Controller'
    case 'worker': return 'Worker'
    default: return nodeType
  }
}
