import { StrictMode, type ComponentType } from 'react'
import { createRoot } from 'react-dom/client'
import './App.css'

const ROUTES: Record<string, () => Promise<{ default: ComponentType }>> = {
  '/': () => import('./modules/dashboard'),
}

const NotFound = () => (
  <main style={{ padding: 24, fontFamily: 'sans-serif' }}>
    <h2>Glossary Keeper</h2>
    <p>
      No module matches <code>{location.pathname}</code>.
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
