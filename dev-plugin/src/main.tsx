import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import ToolsPanel from './ToolsPanel.tsx'
import KeyEditTab from './KeyEditTab.tsx'

const route = window.location.pathname.replace(/\/+$/, '') || '/'
const Root =
  route === '/tools-panel'
    ? ToolsPanel
    : route === '/key-edit-tab/audit'
      ? KeyEditTab
      : App

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Root />
  </StrictMode>,
)
