import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useEffect } from 'react';
import * as Sentry from '@sentry/browser';
import { useGlobalLoading } from './GlobalLoading';
import { useIdentify } from 'tg.hooks/useIdentify';
import { useIsFetching, useIsMutating } from 'react-query';
import { useConfig, useUser } from 'tg.globalContext/helpers';
import { usePosthogInit } from 'tg.hooks/usePosthog';
import { usePlausible } from 'tg.hooks/plausible';
import { CustomOptions } from 'tg.service/http/useQueryApi';

export const MandatoryDataProvider = (props: any) => {
  const userData = useUser();
  const config = useConfig();

  const isFetching = useGlobalContext((c) => c.initialData.isFetching);

  const isGloballyFetching = useIsFetching({
    predicate(query) {
      return !(query.options as unknown as CustomOptions).noGlobalLoading;
    },
  });
  const isGloballyMutating = useIsMutating({
    predicate(query) {
      return !(query.options as unknown as CustomOptions).noGlobalLoading;
    },
  });

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

  usePosthogInit();
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
