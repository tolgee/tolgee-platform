import React, { FC } from 'react';
import CssBaseline from '@material-ui/core/CssBaseline';
import { Theme, ThemeProvider } from '@material-ui/core/styles';
import { TolgeeProvider } from '@tolgee/react';
import { UI } from '@tolgee/ui';
import ReactDOM from 'react-dom';
import { QueryClient, QueryClientProvider } from 'react-query';
import { ReactQueryDevtools } from 'react-query/devtools';
import { Provider } from 'react-redux';
import 'reflect-metadata';
import 'regenerator-runtime/runtime';
import { container } from 'tsyringe';

import { App } from './component/App';
import ErrorBoundary from './component/ErrorBoundary';
import { FullPageLoading } from './component/common/FullPageLoading';
import reportWebVitals from './reportWebVitals';
import { DispatchService } from './service/DispatchService';
import configureStore from './store';
import { GlobalLoading, LoadingProvider } from 'tg.component/GlobalLoading';
import { GlobalErrorModal } from 'tg.component/GlobalErrorModal';
import { BottomPanelProvider } from 'tg.component/bottomPanel/BottomPanelContext';
import { ThemeService } from './service/ThemeService';

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
const Wrapper: FC = () => {
  const [theme, setTheme] = React.useState<Theme>(
    container.resolve(ThemeService).theme
  );
  container.resolve(ThemeService).setTheme = setTheme;

  return (
    <React.Suspense fallback={<FullPageLoading />}>
      <TolgeeProvider
        apiUrl={process.env.REACT_APP_TOLGEE_API_URL}
        apiKey={process.env.REACT_APP_TOLGEE_API_KEY}
        ui={process.env.REACT_APP_TOLGEE_API_KEY ? UI : undefined}
        staticData={{
          en: () => import('./i18n/en.json'),
          // @ts-ignore
          es: () => import('./i18n/es.json'),
          cs: () => import('./i18n/cs.json'),
          fr: () => import('./i18n/fr.json'),
        }}
        loadingFallback={<FullPageLoading />}
        availableLanguages={['en', 'cs', 'es', 'fr']}
        wrapperMode="invisible"
      >
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <Provider store={store}>
            <QueryClientProvider client={queryClient}>
              {/* @ts-ignore */}
              <ErrorBoundary>
                <SnackbarProvider data-cy="global-snackbars">
                  <LoadingProvider>
                    <BottomPanelProvider>
                      <GlobalLoading />
                      <App />
                      <GlobalErrorModal />
                    </BottomPanelProvider>
                  </LoadingProvider>
                </SnackbarProvider>
              </ErrorBoundary>
              <ReactQueryDevtools />
            </QueryClientProvider>
          </Provider>
        </ThemeProvider>
      </TolgeeProvider>
    </React.Suspense>
  );
};

ReactDOM.render(<Wrapper />, document.getElementById('root'));

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
