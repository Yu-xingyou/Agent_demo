<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  // low: 轻量背景；heavy: 重粒子（任务/趋势页）
  density: { type: String, default: 'low' },
})

const canvas = ref(null)
let ctx = null
let raf = 0
let particles = []
let w = 0
let h = 0
let dpr = 1
const mouse = { x: -9999, y: -9999 }

const PALETTE = ['#667eea', '#764ba2', '#22d3ee', '#ec4899', '#34d399']

function config() {
  const heavy = props.density === 'heavy'
  return {
    count: heavy ? 70 : 34,
    maxRadius: heavy ? 2.8 : 2.0,
    speed: heavy ? 0.55 : 0.32,
    link: heavy,
    linkDist: heavy ? 150 : 120,
    alpha: heavy ? 0.34 : 0.22,
  }
}

function resize() {
  if (!canvas.value) return
  dpr = window.devicePixelRatio || 1
  w = canvas.value.clientWidth
  h = canvas.value.clientHeight
  canvas.value.width = w * dpr
  canvas.value.height = h * dpr
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

function makeParticle() {
  const c = config()
  return {
    x: Math.random() * w,
    y: Math.random() * h,
    r: Math.random() * c.maxRadius + 0.6,
    vx: (Math.random() - 0.5) * c.speed,
    vy: (Math.random() - 0.5) * c.speed,
    color: PALETTE[Math.floor(Math.random() * PALETTE.length)],
  }
}

function init() {
  particles = []
  const c = config()
  for (let i = 0; i < c.count; i++) particles.push(makeParticle())
}

function step() {
  const c = config()
  ctx.clearRect(0, 0, w, h)
  for (let i = 0; i < particles.length; i++) {
    const p = particles[i]
    p.x += p.vx
    p.y += p.vy
    // 鼠标轻微排斥
    const dx = p.x - mouse.x
    const dy = p.y - mouse.y
    const d2 = dx * dx + dy * dy
    if (d2 < 120 * 120 && d2 > 0.01) {
      const d = Math.sqrt(d2)
      const force = (120 - d) / 120 * 0.6
      p.x += (dx / d) * force
      p.y += (dy / d) * force
    }
    if (p.x < -10) p.x = w + 10
    if (p.x > w + 10) p.x = -10
    if (p.y < -10) p.y = h + 10
    if (p.y > h + 10) p.y = -10

    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = p.color
    ctx.globalAlpha = c.alpha
    ctx.fill()
  }
  // 连线（重粒子模式）
  if (c.link) {
    ctx.globalAlpha = 0.08
    ctx.strokeStyle = '#7e88a3'
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const a = particles[i]
        const b = particles[j]
        const dx = a.x - b.x
        const dy = a.y - b.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < c.linkDist) {
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.stroke()
        }
      }
    }
  }
  ctx.globalAlpha = 1
  raf = requestAnimationFrame(step)
}

function onMove(e) {
  const rect = canvas.value.getBoundingClientRect()
  mouse.x = e.clientX - rect.left
  mouse.y = e.clientY - rect.top
}
function onLeave() {
  mouse.x = -9999
  mouse.y = -9999
}

onMounted(() => {
  ctx = canvas.value.getContext('2d')
  resize()
  init()
  step()
  window.addEventListener('resize', resize)
  canvas.value.addEventListener('mousemove', onMove)
  canvas.value.addEventListener('mouseleave', onLeave)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
  if (canvas.value) {
    canvas.value.removeEventListener('mousemove', onMove)
    canvas.value.removeEventListener('mouseleave', onLeave)
  }
})

watch(() => props.density, () => {
  if (ctx) init()
})
</script>

<template>
  <canvas ref="canvas" class="particle-canvas"></canvas>
</template>

<style scoped>
.particle-canvas {
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}
</style>
