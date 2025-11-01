<!-- Confetti.vue -->
<template>
  <canvas ref="canvas" class="pointer-events-none block w-full h-full"></canvas>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, defineProps, defineExpose, watchEffect } from 'vue'

type Vec = { x: number; y: number }

const props = defineProps<{
  autoplay?: boolean
  particles?: number
  durationMs?: number
  colors?: string[]
  origin?: Vec // 0..1 relatieve positie in het canvas
}>()

const canvas = ref<HTMLCanvasElement | null>(null)
let ctx: CanvasRenderingContext2D | null = null
let raf = 0

// Defaults
const P = {
  particles: props.particles ?? 225,
  durationMs: props.durationMs ?? 1500,
  colors: props.colors ?? ['#FF5252', '#FFB300', '#4CAF50', '#42A5F5', '#AB47BC', '#FFEB3B'],
  origin: props.origin ?? { x: 0.5, y: 0.3 },
}

function resize() {
  if (!canvas.value) return
  // pixel ratio aware
  const dpr = Math.max(1, Math.min(2, window.devicePixelRatio || 1))
  const rect = canvas.value.getBoundingClientRect()
  canvas.value.width = Math.round(rect.width * dpr)
  canvas.value.height = Math.round(rect.height * dpr)
  ;(canvas.value.style as any).imageRendering = 'pixelated'
  ctx?.setTransform(dpr, 0, 0, dpr, 0, 0) // render op CSS pixels
}

type Particle = {
  x: number; y: number
  vx: number; vy: number
  w: number; h: number
  rot: number; vr: number
  color: string
  life: number // 0..1
}

function spawn(width: number, height: number): Particle[] {
  const startX = width * P.origin.x
  const startY = height * P.origin.y
  const out: Particle[] = []
  for (let i = 0; i < P.particles; i++) {
    const angle = (Math.random() * 0.8 + 0.1) * Math.PI // 18°..162°
    const speed = 6 + Math.random() * 8
    const size = 4 + Math.random() * 6
    out.push({
      x: startX, y: startY,
      vx: Math.cos(angle) * speed * (Math.random() < 0.5 ? -1 : 1),
      vy: -Math.sin(angle) * speed,
      w: size, h: size * (0.6 + Math.random() * 0.8),
      rot: Math.random() * Math.PI * 2,
      vr: (Math.random() - 0.5) * 0.3,
      color: P.colors[i % P.colors.length],
      life: 1,
    })
  }
  return out
}

function play() {
  if (!canvas.value || !ctx) return
  const width = canvas.value.width
  const height = canvas.value.height
  const parts = spawn(width, height)

  const start = performance.now()
  cancelAnimationFrame(raf)

  const step = (t: number) => {
    if (!ctx || !canvas.value) return
    const elapsed = t - start
    const k = Math.min(1, elapsed / P.durationMs)

    // clear
    ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)

    const gravity = 0.25
    const drag = 0.992

    for (const p of parts) {
      // physics
      p.vx *= drag
      p.vy = p.vy * drag + gravity
      p.x += p.vx
      p.y += p.vy
      p.rot += p.vr
      // fade-out richting einde
      p.life = 1 - k

      // draw
      ctx.save()
      ctx.translate(p.x, p.y)
      ctx.rotate(p.rot)
      ctx.globalAlpha = Math.max(0, p.life)
      ctx.fillStyle = p.color
      ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h)
      ctx.restore()
    }

    if (k < 1) {
      raf = requestAnimationFrame(step)
    } else {
      ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
    }
  }

  raf = requestAnimationFrame(step)
}

defineExpose({ play })

onMounted(() => {
  ctx = canvas.value?.getContext('2d') ?? null
  resize()
  window.addEventListener('resize', resize)
  if (props.autoplay) play()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
})

watchEffect(() => {
  // props mutaties opvangen voor nieuwe bursts
  P.particles = props.particles ?? P.particles
  P.durationMs = props.durationMs ?? P.durationMs
  P.colors = props.colors ?? P.colors
  P.origin = props.origin ?? P.origin
})
</script>
