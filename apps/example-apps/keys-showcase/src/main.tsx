import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { KeysShowcase } from './KeysShowcase'
import './App.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <KeysShowcase />
  </StrictMode>
)
