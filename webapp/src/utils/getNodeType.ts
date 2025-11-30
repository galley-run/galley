export default function(nodeType: 'worker' | 'controller' | 'controller+worker' | string) {
  switch (nodeType) {
    case 'controller+worker': return 'Controller & Worker'
    case 'controller': return 'Controller'
    case 'worker': return 'Worker'
    default: return nodeType
  }
}
