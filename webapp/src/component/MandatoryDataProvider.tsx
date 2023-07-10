import { useConfig, useUser } from 'tg.globalContext/helpers';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useEffect } from 'react';
import * as Sentry from '@sentry/browser';
import { useGlobalLoading } from './GlobalLoading';

const POSTHOG_INITIALIZED_WINDOW_PROPERTY = 'postHogInitialized';
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

  async function initPostHog() {
    if (!window[POSTHOG_INITIALIZED_WINDOW_PROPERTY]) {
      const posthog = await import('posthog-js').then((m) => m.default);
      if (config?.postHogApiKey) {
        posthog.init(config.postHogApiKey, {
          api_host: config?.postHogHost || undefined,
        });
        window[POSTHOG_INITIALIZED_WINDOW_PROPERTY] = true;
        posthog.identify(userData!.id.toString(), {
          name: userData!.username,
          email: userData!.username,
        });
      }
    }
  }

  useEffect(() => {
    Sentry.setUser({
      email: userData!.username,
      id: userData!.id.toString(),
    });
    initPostHog();
  }, [userData?.id, config?.postHogApiKey]);

  useGlobalLoading(isFetching || isLoading);

  if (isLoading) {
    return null;
  } else {
    return props.children;
  }
};
