import React from 'react';
import CssBaseline from '@mui/material/CssBaseline';
import { StyledEngineProvider } from '@mui/material/styles';
import { TolgeeProvider } from '@tolgee/react';
import ReactDOM from 'react-dom';
import { QueryClient, QueryClientProvider } from 'react-query';
import { BrowserRouter } from 'react-router-dom';
import { ReactQueryDevtools } from 'react-query/devtools';
import { Provider } from 'react-redux';
import 'reflect-metadata';
import 'regenerator-runtime/runtime';
import { container } from 'tsyringe';

import { GlobalLoading, LoadingProvider } from 'tg.component/GlobalLoading';
import { GlobalErrorModal } from 'tg.component/GlobalErrorModal';
import { BottomPanelProvider } from 'tg.component/bottomPanel/BottomPanelContext';
import { TopBarProvider } from 'tg.component/layout/TopBar/TopBarContext';
import { GlobalProvider } from 'tg.globalContext/GlobalContext';
import { App } from './component/App';
import ErrorBoundary from './component/ErrorBoundary';
import { FullPageLoading } from './component/common/FullPageLoading';
import { ThemeProvider } from './ThemeProvider';

import reportWebVitals from './reportWebVitals';
import { DispatchService } from './service/DispatchService';
import configureStore from './store';

const store = configureStore();

const SnackbarProvider = React.lazy(() =>
  import(/* webpackChunkName: "notistack" */ 'notistack').then((module) => ({
    default: module.SnackbarProvider,
  }))
);

container.resolve(DispatchService).store = store;

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
      retry: false,
    },
  },
});

const MainWrapper = () => {
  return (
    <React.Suspense fallback={<FullPageLoading />}>
      <TolgeeProvider
        apiUrl={process.env.REACT_APP_TOLGEE_API_URL}
        apiKey={process.env.REACT_APP_TOLGEE_API_KEY}
        staticData={{
          en: () => import('./i18n/en.json'),
          // @ts-ignore
          es: () => import('./i18n/es.json'),
          cs: () => import('./i18n/cs.json'),
          fr: () => import('./i18n/fr.json'),
          de: () => import('./i18n/de.json'),
        }}
        loadingFallback={<FullPageLoading />}
      >
        <BrowserRouter>
          <StyledEngineProvider injectFirst>
            <ThemeProvider>
              <CssBaseline />
              <Provider store={store}>
                <QueryClientProvider client={queryClient}>
                  {/* @ts-ignore */}
                  <ErrorBoundary>
                    <SnackbarProvider data-cy="global-snackbars">
                      <LoadingProvider>
                        <GlobalProvider>
                          <BottomPanelProvider>
                            <TopBarProvider>
                              <GlobalLoading />
                              <App />
                              <GlobalErrorModal />
                            </TopBarProvider>
                          </BottomPanelProvider>
                        </GlobalProvider>
                      </LoadingProvider>
                    </SnackbarProvider>
                  </ErrorBoundary>
                  <ReactQueryDevtools />
                </QueryClientProvider>
              </Provider>
            </ThemeProvider>
          </StyledEngineProvider>{' '}
        </BrowserRouter>
      </TolgeeProvider>
    </React.Suspense>
  );
};

ReactDOM.render(<MainWrapper />, document.getElementById('root'));

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
