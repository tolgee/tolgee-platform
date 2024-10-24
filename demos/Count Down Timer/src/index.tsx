import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import { Tolgee, DevTools, TolgeeProvider, FormatSimple , BackendFetch} from "@tolgee/react";


const tolgee = Tolgee()
  .use(DevTools())
  .use(BackendFetch())
  .use(FormatSimple())
  .init({
    language: 'en',
    apiUrl: process.env.REACT_APP_TOLGEE_API_URL,
    apiKey: process.env.REACT_APP_API_KEY,
  });

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);
root.render(
  <React.StrictMode>
    <TolgeeProvider
      tolgee={tolgee}>
      <App />
    </TolgeeProvider>
  </React.StrictMode>
);

 


 

