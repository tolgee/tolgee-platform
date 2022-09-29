import { useConfig, useUser } from 'tg.globalContext/helpers';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useEffect, useState } from 'react';
import API from '@openreplay/tracker';
import * as Sentry from '@sentry/browser';
import { useGlobalLoading } from './GlobalLoading';

export const MandatoryDataProvider = (props: any) => {
  const config = useConfig();
  const userData = useUser();
  const isLoading = useGlobalContext((v) => v.isLoading);
  const isFetching = useGlobalContext((v) => v.isFetching);
  const [openReplayTracker, setOpenReplayTracker] = useState(
    undefined as undefined | API
  );

  useEffect(() => {
    if (config?.clientSentryDsn) {
      Sentry.init({ dsn: config.clientSentryDsn });
      // eslint-disable-next-line no-console
      console.info('Using Sentry!');
    }
  }, [config?.clientSentryDsn]);

  useEffect(() => {
    const openReplayApiKey = config?.openReplayApiKey;
    if (openReplayApiKey && !window.openReplayTracker) {
      import('@openreplay/tracker').then(({ default: Tracker }) => {
        window.openReplayTracker = new Tracker({
          projectKey: openReplayApiKey,
          __DISABLE_SECURE_MODE:
            process.env.NODE_ENV === 'development' ? true : undefined,
        });
        setOpenReplayTracker(window.openReplayTracker);
        window.openReplayTracker.start();
      });
    }
    setOpenReplayTracker(window.openReplayTracker);
  }, [config?.clientSentryDsn, config?.openReplayApiKey]);

  useEffect(() => {
    if (userData && openReplayTracker) {
      openReplayTracker.setUserID(userData.username);
      setTimeout(() => {
        openReplayTracker?.setUserID(userData.username);
      }, 2000);
    }
  }, [userData, openReplayTracker]);

  useGlobalLoading(isFetching || isLoading);

  if (isLoading) {
    return null;
  } else {
    return props.children;
  }
};
