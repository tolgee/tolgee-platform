import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import ToolsPanel from './ToolsPanel.tsx'
import ToolsPanelEmpty from './ToolsPanelEmpty.tsx'
import KeyEditTab from './KeyEditTab.tsx'
import ModalExplain from './ModalExplain.tsx'

const route = window.location.pathname.replace(/\/+$/, '') || '/'
const Root =
  route === '/tools-panel'
    ? ToolsPanel
    : route === '/tools-panel-empty'
      ? ToolsPanelEmpty
      : route === '/key-edit-tab/audit'
      ? KeyEditTab
      : route === '/modal/explain'
        ? ModalExplain
        : App

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Root />
  </StrictMode>,
)
