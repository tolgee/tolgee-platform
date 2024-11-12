/* eslint-disable no-unused-vars */
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import './index.css'
import { Tolgee, DevTools, TolgeeProvider, BackendFetch, FormatSimple } from "@tolgee/react";

const tolgee = Tolgee()
  .use(DevTools())
  .use(BackendFetch())
  .use(FormatSimple())
  .init({
    language: 'en',
    apiUrl: import.meta.env.VITE_APP_TOLGEE_API_URL,
    apiKey: import.meta.env.VITE_APP_TOLGEE_API_KEY,
  });

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <TolgeeProvider
      tolgee={tolgee}> 
      <App />
    </TolgeeProvider>
  </StrictMode>,
)
