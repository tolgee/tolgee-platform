import { useEffect } from 'react';
import { useConfig } from 'tg.globalContext/helpers';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const useWindowTitle = (title: string) => {
  const config = useConfig();
  const notificationCount = useGlobalContext((c) => c.unseenNotificationCount);

  useEffect(() => {
    if (title) {
      const oldTitle = window.document.title;
      window.document.title = `${title} | ${config.appName}`;
      if (notificationCount) {
        window.document.title += ` (${notificationCount})`;
      }
      return () => {
        window.document.title = oldTitle;
      };
    }
  }, [title, config.appName, notificationCount]);
};
