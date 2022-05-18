import { useEffect } from 'react';
import { useConfig } from './useConfig';

export const useWindowTitle = (title: string) => {
  const config = useConfig();

  useEffect(() => {
    if (title) {
      const oldTitle = window.document.title;
      window.document.title = `${config.appName} | ${title}`;
      return () => {
        window.document.title = oldTitle;
      };
    }
  }, [title, config.appName]);
};
