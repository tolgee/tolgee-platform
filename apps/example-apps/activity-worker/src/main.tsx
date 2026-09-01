import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ActivityWorker } from './ActivityWorker'
import './App.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ActivityWorker />
  </StrictMode>
)
