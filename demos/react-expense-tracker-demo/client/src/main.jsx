import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import './index.css'

import { Tolgee, TolgeeProvider, BackendFetch, DevTools } from '@tolgee/react';
import { FormatIcu } from '@tolgee/format-icu';

const tolgee = Tolgee()
  .use(DevTools())
  .use(FormatIcu())
  .use(BackendFetch())
  .init({
    availableLanguages: ['en', 'hi-IN', 'it-IT', 'mr-IN', 'ru-RU'],
    apiUrl: import.meta.env.VITE_APP_TOLGEE_API_URL,
    apiKey: import.meta.env.VITE_APP_TOLGEE_API_KEY,
    projectId: import.meta.env.VITE_APP_TOLGEE_PROJECT_ID,
    fallbackLanguage: 'en',
    defaultLanguage: 'en',
  });

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <TolgeeProvider tolgee={tolgee} fallback="Loading...">
      <App />
    </TolgeeProvider>
  </StrictMode>,
)
