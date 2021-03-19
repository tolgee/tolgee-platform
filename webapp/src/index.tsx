import 'reflect-metadata';
import 'regenerator-runtime/runtime';
import {Provider} from 'react-redux';

import * as React from 'react';
import * as ReactDOM from 'react-dom';

import configureStore from './store';

import {container} from 'tsyringe';

import {dispatchService} from './service/dispatchService';

import ErrorBoundary from "./component/ErrorBoundary";
import RubikWoff2 from './fonts/Rubik/Rubik-Regular.woff2';
import RighteousLatinWoff2 from './fonts/Righteous/righteous-latin.woff2';
import RighteousLatinExtWoff2 from './fonts/Righteous/righteous-latin-ext.woff2';

import {createMuiTheme, ThemeProvider} from '@material-ui/core/styles';
import {TolgeeProvider} from "@tolgee/react";
import {UI} from "@tolgee/ui";
import {App} from "./component/App";
import {FullPageLoading} from "./component/common/FullPageLoading";
import './yupLocalization';

const store = configureStore();

const SnackbarProvider = React.lazy(() => import(/* webpackChunkName: "notistack" */ 'notistack')
    .then(module => ({"default": module.SnackbarProvider})));

container.resolve(dispatchService).store = store;

const rubik = {
    fontFamily: 'Rubik',
    fontStyle: 'normal',
    fontDisplay: 'swap',
    fontWeight: 400,
    src: `
    local('Rubik'),
    local('Rubik-Regular'),
    url(${RubikWoff2}) format('woff2')
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
        "U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+2000-206F, U+2074, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD"
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
    unicodeRange: "U+0100-024F, U+0259, U+1E00-1EFF, U+2020, U+20A0-20AB, U+20AD-20CF, U+2113, U+2C60-2C7F, U+A720-A7FF"
};

const theme = createMuiTheme({
    typography: {
        fontFamily: 'Rubik, Arial',
    },
    palette: {
        primary: {
            main: "#822B55"
        },
        secondary: {
            main: "#2B5582"
        }
    },
    overrides: {
        MuiCssBaseline: {
            // @ts-ignore
            '@global': {
                // @ts-ignore
                '@font-face': [rubik, righteousLatinExt, righteousLatin]
            },
        },
        MuiButton: {
            root: {
                borderRadius: 3,
            },
        },
    },
});

ReactDOM.render(
    <React.Suspense fallback={<FullPageLoading/>}>
        <TolgeeProvider
            apiUrl={environment.tolgeeApiUrl}
            apiKey={environment.tolgeeApiKey}
            ui={environment.tolgeeApiKey ? UI : undefined}
            filesUrlPrefix="/i18n/"
            loadingFallback={<FullPageLoading/>}
            availableLanguages={["en", "cs"]}
        >
            <ThemeProvider theme={theme}>
                <Provider store={store}>
                    {/*
                    // @ts-ignore --  Type '{ children: Element; }' has no properties ... */}
                    <ErrorBoundary>
                        <SnackbarProvider>
                            <App/>
                        </SnackbarProvider>
                    </ErrorBoundary>
                </Provider>
            </ThemeProvider>
        </TolgeeProvider>
    </React.Suspense>,
    document.getElementById('root')
);
