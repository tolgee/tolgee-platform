// import {  useEffect } from 'react';
// import RecipePage from './components/RecipePage'

// export default function App() {
//   useEffect(() => {
//     console.log('App component mounted')
//   }, [])

//   return (
//     <div className="App">
 
//       <RecipePage />
//     </div>
//   )
// }
import { Tolgee, TolgeeProvider, BackendFetch, DevTools } from "@tolgee/react";
import { FormatIcu } from "@tolgee/format-icu";
import { useEffect } from 'react';
import RecipePage from './components/RecipePage';
import { Router } from 'lucide-react';
import LanguageSwitcher from "./components/LanguageSwitcher";
export const tolgee = Tolgee()
  .use(DevTools())
  .use(FormatIcu())
  .use(BackendFetch())
  .init({
    availableLanguages: ["en", "cs", "fr", "de"],
    apiUrl: import.meta.env.VITE_APP_TOLGEE_API_URL,
    apiKey: import.meta.env.VITE_APP_TOLGEE_API_KEY,
    projectId: import.meta.env.VITE_APP_TOLGEE_PROJECT_ID,
    fallbackLanguage: "en",
    defaultLanguage: "en",
  });

export const App = () => {
  return (
    <TolgeeProvider tolgee={tolgee}>
      <div>
        <LanguageSwitcher />
        <RecipePage />
      </div>
    </TolgeeProvider>
  );
};

export default App;