import { StrictMode, type ComponentType } from 'react'
import { createRoot } from 'react-dom/client'
import './App.css'

// Each iframe module is lazy-imported by URL path. The scaffold only
// generates folders for the modules the wizard selected; unselected
// paths show a tiny fallback.
const ROUTES: Record<string, () => Promise<{ default: ComponentType }>> = {
  '/tools-panel': () => import('./modules/toolsPanel'),
}

const NotFound = () => (
  <main style={{ padding: 24, fontFamily: 'sans-serif' }}>
    <h2>Back Translate</h2>
    <p>
      No module matches <code>{location.pathname}</code>. Add a route in{' '}
      <code>src/main.tsx</code> and a manifest entry in{' '}
      <code>server/manifest.template.json</code>.
    </p>
  </main>
)

async function mount() {
  const root = createRoot(document.getElementById('root')!)
  const loader = ROUTES[location.pathname]
  if (!loader) {
    root.render(
      <StrictMode>
        <NotFound />
      </StrictMode>
    )
    return
  }
  const { default: Component } = await loader()
  root.render(
    <StrictMode>
      <Component />
    </StrictMode>
  )
}

mount()
