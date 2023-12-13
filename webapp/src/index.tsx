import React, { Suspense } from 'react';
import CssBaseline from '@mui/material/CssBaseline';
import { StyledEngineProvider } from '@mui/material/styles';
import {
  DevTools,
  LanguageDetector,
  Tolgee,
  TolgeeProvider,
} from '@tolgee/react';
import { FormatIcu } from '@tolgee/format-icu';
import ReactDOM from 'react-dom';
import { QueryClientProvider } from 'react-query';
import { BrowserRouter } from 'react-router-dom';
import { ReactQueryDevtools } from 'react-query/devtools';
import { Provider } from 'react-redux';
import 'reflect-metadata';
import 'regenerator-runtime/runtime';
import { container } from 'tsyringe';

import { GlobalLoading, LoadingProvider } from 'tg.component/GlobalLoading';
import { GlobalErrorModal } from 'tg.component/GlobalErrorModal';
import { BottomPanelProvider } from 'tg.component/bottomPanel/BottomPanelContext';
import { GlobalProvider } from 'tg.globalContext/GlobalContext';
import { App } from './component/App';
import ErrorBoundary from './component/ErrorBoundary';
import { FullPageLoading } from './component/common/FullPageLoading';
import { ThemeProvider } from './ThemeProvider';

import reportWebVitals from './reportWebVitals';
import { DispatchService } from './service/DispatchService';
import configureStore from './store';
import { MuiLocalizationProvider } from 'tg.component/MuiLocalizationProvider';
import { languageStorage, queryClient } from './initialSetup';

const store = configureStore();

const SnackbarProvider = React.lazy(() =>
  import(/* webpackChunkName: "notistack" */ 'notistack').then((module) => ({
    default: module.SnackbarProvider,
  }))
);

container.resolve(DispatchService).store = store;

const tolgee = Tolgee()
  .use(DevTools())
  .use(FormatIcu())
  .use(LanguageDetector())
  .use(languageStorage)
  .init({
    defaultLanguage: 'en',
    fallbackLanguage: 'en',
    apiUrl: process.env.REACT_APP_TOLGEE_API_URL,
    apiKey: process.env.REACT_APP_TOLGEE_API_KEY,
    staticData: {
      en: () => import('./i18n/en.json'),
      es: () => import('./i18n/es.json'),
      cs: () => import('./i18n/cs.json'),
      fr: () => import('./i18n/fr.json'),
      de: () => import('./i18n/de.json'),
      pt: () => import('./i18n/pt.json'),
      da: () => import('./i18n/da.json'),
    },
  });

const MainWrapper = () => {
  return (
    <StyledEngineProvider injectFirst>
      <ThemeProvider>
        <CssBaseline />
        <LoadingProvider>
          <GlobalLoading />
          <Suspense fallback={<FullPageLoading />}>
            <TolgeeProvider
              tolgee={tolgee}
              fallback={<FullPageLoading />}
              options={{ useSuspense: false }}
            >
              <BrowserRouter>
                <Provider store={store}>
                  <QueryClientProvider client={queryClient}>
                    {/* @ts-ignore */}
                    <ErrorBoundary>
                      <SnackbarProvider data-cy="global-snackbars">
                        <GlobalProvider>
                          <BottomPanelProvider>
                            <MuiLocalizationProvider>
                              <App />
                            </MuiLocalizationProvider>
                            <GlobalErrorModal />
                          </BottomPanelProvider>
                        </GlobalProvider>
                      </SnackbarProvider>
                    </ErrorBoundary>
                    <ReactQueryDevtools />
                  </QueryClientProvider>
                </Provider>
              </BrowserRouter>
            </TolgeeProvider>
          </Suspense>
        </LoadingProvider>
      </ThemeProvider>
    </StyledEngineProvider>
  );
};

ReactDOM.render(<MainWrapper />, document.getElementById('root'));

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
