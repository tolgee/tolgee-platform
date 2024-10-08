import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useEffect } from 'react';
import * as Sentry from '@sentry/browser';
import { useGlobalLoading } from './GlobalLoading';
import { useIdentify } from 'tg.hooks/useIdentify';
import { useIsFetching, useIsMutating } from 'react-query';
import { useConfig, useUser } from 'tg.globalContext/helpers';
import { usePosthog } from 'tg.hooks/usePosthog';
import { usePlausible } from 'tg.hooks/plausible';

export const MandatoryDataProvider = (props: any) => {
  const userData = useUser();
  const config = useConfig();

  const isFetching = useGlobalContext((c) => c.initialData.isFetching);

  const isGloballyFetching = useIsFetching();
  const isGloballyMutating = useIsMutating();

  useGlobalLoading(
    Boolean(isGloballyFetching || isGloballyMutating || isFetching)
  );

  useIdentify(userData?.id);

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

  usePosthog();
  usePlausible();

  useEffect(() => {
    if (userData) {
      Sentry.setUser({
        email: userData.username,
        id: userData.id.toString(),
      });
    }
  }, [userData?.id]);

  return props.children;
};
