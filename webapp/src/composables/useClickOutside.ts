import { onBeforeUnmount } from 'vue'

type TargetGetter = () => (HTMLElement | null | undefined)[] | HTMLElement | null | undefined

type Entry = {
  getTargets: TargetGetter
  onOutside: (ev: Event) => void
}

const entries = new Set<Entry>()
let attached = false

function handler(ev: Event) {
  const t = ev.target as Node | null
  for (const entry of entries) {
    const targetsRaw = entry.getTargets()
    const targets = Array.isArray(targetsRaw) ? targetsRaw : [targetsRaw]
    const isInside = targets.some(raw => {
      const el = (raw as { $el?: Node })?.$el ?? raw
      return el instanceof Node && (t ? el.contains(t) : false)
    })
    if (!isInside) entry.onOutside(ev)
  }
}

export function useClickOutside(getTargets: TargetGetter, onOutside: (ev: Event) => void) {
  const entry: Entry = { getTargets, onOutside }
  entries.add(entry)

  if (!attached) {
    // pointerdown sluit eerder (voor focus/klik) en werkt ook op touch
    document.addEventListener('pointerdown', handler, { passive: true, capture: true })
    attached = true
  }

  onBeforeUnmount(() => {
    entries.delete(entry)
    if (attached && entries.size === 0) {
      document.removeEventListener('pointerdown', handler, { capture: true })
      attached = false
    }
  })
}
