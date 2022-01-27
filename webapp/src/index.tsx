import React from 'react';
import CssBaseline from '@material-ui/core/CssBaseline';
import { ThemeProvider } from '@material-ui/core/styles';
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
// @ts-ignore
import RighteousLatinExtWoff2 from './fonts/Righteous/righteous-latin-ext.woff2';
// @ts-ignore
import RighteousLatinWoff2 from './fonts/Righteous/righteous-latin.woff2';
// @ts-ignore
import RubikWoff2 from './fonts/Rubik/Rubik-Regular.woff2';
import reportWebVitals from './reportWebVitals';
import { DispatchService } from './service/DispatchService';
import configureStore from './store';
import { createTheme } from '@material-ui/core';
import { GlobalLoading, LoadingProvider } from 'tg.component/GlobalLoading';
import { GlobalErrorModal } from 'tg.component/GlobalErrorModal';
import { BottomPanelProvider } from 'tg.component/bottomPanel/BottomPanelContext';
import { TOP_BAR_HEIGHT } from 'tg.component/layout/TopBar';

const store = configureStore();

const SnackbarProvider = React.lazy(() =>
  import(/* webpackChunkName: "notistack" */ 'notistack').then((module) => ({
    default: module.SnackbarProvider,
  }))
);

container.resolve(DispatchService).store = store;

const rubik = {
  fontFamily: 'Rubik',
  fontStyle: 'normal',
  fontDisplay: 'swap',
  fontWeight: 400,
  src: `
    url(${RubikWoff2}) format('woff2')
    local('Rubik'),
    local('Rubik-Regular'),
  `,
  unicodeRange:
    'U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+2000-206F, U+2074, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF',
};

const righteousLatin = {
  fontFamily: 'Righteous',
  fontStyle: 'normal',
  fontDisplay: 'swap',
  fontWeight: 400,
  src: `
    local('Righteous'),
    local('Righteous-Regular'),
    url(${RighteousLatinWoff2}) format('woff2')
  `,
  unicodeRange:
    'U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+2000-206F, U+2074, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD',
};

const righteousLatinExt = {
  fontFamily: 'Righteous',
  fontStyle: 'normal',
  fontDisplay: 'swap',
  fontWeight: 400,
  src: `
    local('Righteous'),
    local('Righteous-Regular'),
    url(${RighteousLatinExtWoff2}) format('woff2')
  `,
  unicodeRange:
    'U+0100-024F, U+0259, U+1E00-1EFF, U+2020, U+20A0-20AB, U+20AD-20CF, U+2113, U+2C60-2C7F, U+A720-A7FF',
};

const theme = createTheme({
  typography: {
    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol"',
    h1: {
      fontSize: 42,
      fontWeight: 300,
    },
    h2: {
      fontSize: 36,
      fontWeight: 300,
    },
    h3: {
      fontSize: 28,
      fontWeight: 400,
    },
    h4: {
      fontSize: 24,
      fontWeight: 400,
    },
    h5: {
      fontSize: 20,
      fontWeight: 400,
    },
    h6: {
      fontSize: 18,
      fontWeight: 500,
    },
    subtitle1: {
      fontSize: 18,
      fontWeight: 400,
    },
    subtitle2: {
      fontSize: 16,
      fontWeight: 500,
    },
    body1: {
      fontSize: 16,
      fontWeight: 400,
    },
    body2: {
      fontSize: 15,
      fontWeight: 400,
    },
    button: {
      fontSize: 14,
      fontWeight: 500,
    },
    caption: {
      fontSize: 12,
      fontWeight: 400,
    },
    overline: {
      fontWeight: 400,
      fontSize: 10,
    },
  },
  palette: {
    primary: {
      main: '#822B55',
    },
    secondary: {
      main: '#2B5582',
    },
    text: {
      secondary: '#808080',
    },
    lightDivider: {
      main: 'rgba(0, 0, 0, 0.12)',
    },
    extraLightDivider: {
      main: 'rgba(0, 0, 0, 0.08)',
    },
    lightBackground: {
      main: '#E5E5E5',
    },
    extraLightBackground: {
      main: '#F8F8F8',
    },
    background: {
      default: 'rgb(255, 255, 255)',
    },
  },
  mixins: {
    toolbar: {
      minHeight: TOP_BAR_HEIGHT,
    },
  },
  overrides: {
    MuiTooltip: {
      tooltip: {
        fontSize: 12,
        boxShadow: '1px 1px 6px rgba(0, 0, 0, 0.25)',
        borderRadius: '11px',
        color: 'black',
        backgroundColor: 'white',
      },
    },
    MuiCssBaseline: {
      // @ts-ignore
      '@global': {
        // @ts-ignore
        '@font-face': [rubik, righteousLatinExt, righteousLatin],
        html: {
          height: '100%',
        },
        body: {
          minHeight: '100%',
          position: 'relative',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'stretch',
        },
        '#root': {
          flexGrow: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'stretch',
        },
      },
    },
    MuiButton: {
      root: {
        borderRadius: 3,
        padding: '6px 16px',
        minHeight: 40,
      },
      sizeSmall: {
        minHeight: 32,
        padding: '4px 16px',
      },
    },
    MuiList: {
      padding: {
        paddingTop: 0,
        paddingBottom: 0,
      },
    },
  },
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
      retry: false,
    },
  },
});

ReactDOM.render(
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
  </React.Suspense>,
  document.getElementById('root')
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
