import { useConfig, useUser } from 'tg.globalContext/helpers';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useEffect } from 'react';
import * as Sentry from '@sentry/browser';
import { useGlobalLoading } from './GlobalLoading';
import { PostHog } from 'posthog-js';
import { getUtmParams } from 'tg.fixtures/utmCookie';

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

  function initPostHog() {
    let postHogPromise: Promise<PostHog> | undefined;
    if (!window[POSTHOG_INITIALIZED_WINDOW_PROPERTY]) {
      const postHogAPIKey = config?.postHogApiKey;
      if (postHogAPIKey) {
        window[POSTHOG_INITIALIZED_WINDOW_PROPERTY] = true;
        postHogPromise = import('posthog-js').then((m) => m.default);
        postHogPromise.then((posthog) => {
          posthog.init(postHogAPIKey, {
            api_host: config?.postHogHost || undefined,
          });
          posthog.identify(userData!.id.toString(), {
            name: userData!.username,
            email: userData!.username,
            ...getUtmParams(),
          });
        });
      }
    }
    return () => {
      postHogPromise?.then((ph) => {
        ph.reset();
      });
      window[POSTHOG_INITIALIZED_WINDOW_PROPERTY] = false;
    };
  }

  useEffect(() => {
    Sentry.setUser({
      email: userData!.username,
      id: userData!.id.toString(),
    });
    return initPostHog();
  }, [userData?.id, config?.postHogApiKey]);

  useGlobalLoading(isFetching || isLoading);

  if (isLoading) {
    return null;
  } else {
    return props.children;
  }
};
