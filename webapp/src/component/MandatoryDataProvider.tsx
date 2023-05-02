import { useConfig, useUser } from 'tg.globalContext/helpers';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useEffect } from 'react';
import * as Sentry from '@sentry/browser';
import { useGlobalLoading } from './GlobalLoading';
import { getCurrentHub } from '@sentry/browser';

const SENTRY_INITIALIZED_WINDOW_PROPERTY = 'sentryInitialized';
export const MandatoryDataProvider = (props: any) => {
  const config = useConfig();
  const userData = useUser();
  const isLoading = useGlobalContext((v) => v.isLoading);
  const isFetching = useGlobalContext((v) => v.isFetching);

  useEffect(() => {
    if (config?.clientSentryDsn) {
      Sentry.init({
        dsn: config.clientSentryDsn,
        replaysSessionSampleRate: 1.0,
        replaysOnErrorSampleRate: 1.0,
      });
      // eslint-disable-next-line no-console
      console.info('Using Sentry!');
    }
  }, [config?.clientSentryDsn]);

  async function initReplay() {
    const { Replay } = await import('@sentry/browser');
    if (!window[SENTRY_INITIALIZED_WINDOW_PROPERTY]) {
      getCurrentHub()
        .getClient()
        ?.addIntegration?.(
          new Replay({
            maskAllText: false,
            maskAllInputs: false,
          })
        );
      window[SENTRY_INITIALIZED_WINDOW_PROPERTY] = true;
      Sentry.setUser({
        email: userData!.username,
        id: userData!.id.toString(),
      });
    }
  }

  useEffect(() => {
    if (userData?.id && config?.clientSentryDsn) {
      // noinspection JSIgnoredPromiseFromCall
      initReplay();
    }
  }, [userData?.id, config?.clientSentryDsn]);

  useGlobalLoading(isFetching || isLoading);

  if (isLoading) {
    return null;
  } else {
    return props.children;
  }
};
