import { useEffect } from 'react';
import { useConfig } from 'tg.globalContext/helpers';

export const useWindowTitle = (title: string) => {
  const config = useConfig();

  useEffect(() => {
    if (title) {
      const oldTitle = window.document.title;
      window.document.title = `${title} | ${config.appName}`;
      return () => {
        window.document.title = oldTitle;
      };
    }
  }, [title, config.appName]);
};
