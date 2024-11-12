/* eslint-disable no-unused-vars */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import './App.css';
import Weather from './components/Weather';
import { useTranslate } from '@tolgee/react';
import { LangSelector } from './components/LanguageDropdown'; 


const queryClient = new QueryClient();

function App() {
  const { t } = useTranslate();
  return (
    <QueryClientProvider client={queryClient}>

      <div className="flex  flex-col items-center gap-5">
        <h1 className='text-5xl text-white font-bold '>{t('app-title')}</h1>
        <div className='align-top '> 
            <LangSelector />
        </div>
        <div>
            <Weather />
        </div>
      </div>
    </QueryClientProvider>
  );
}

export default App;
