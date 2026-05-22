import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import ToolsPanel from './ToolsPanel.tsx'

const route = window.location.pathname.replace(/\/+$/, '') || '/'
const Root = route === '/tools-panel' ? ToolsPanel : App

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Root />
  </StrictMode>,
)
