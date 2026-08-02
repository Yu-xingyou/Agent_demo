import { createServer } from 'vite'
import { renderToString } from 'vue/server-renderer'
import { createSSRApp } from 'vue'
import { createRouter, createMemoryHistory } from 'vue-router'

const server = await createServer({
  root: 'd:/javacode/agent_demo/frontend',
  server: { middlewareMode: true },
  appType: 'custom',
  logLevel: 'error',
})

async function probe(path, file) {
  try {
    const mod = await server.ssrLoadModule(file)
    const Comp = mod.default
    const app = createSSRApp(Comp)
    // 简易 router 桩，避免 useRoute 报错
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: Comp }],
    })
    app.use(router)
    await router.push(path)
    await router.isReady()
    const html = await renderToString(app)
    console.log(`[OK] ${path} -> ${file} (len=${html.length})`)
  } catch (e) {
    console.log(`[ERR] ${path} -> ${file}`)
    console.log('   ', e && e.message ? e.message : e)
    if (e && e.stack) console.log(e.stack.split('\n').slice(0, 4).join('\n'))
  }
}

await probe('/', 'src/views/HomeView.vue')
await probe('/checkin', 'src/views/CheckinView.vue')
await probe('/history', 'src/views/HistoryView.vue')
await probe('/trend', 'src/views/TrendView.vue')
await probe('/ai-chat', 'src/views/AiChatView.vue')

await server.close()
