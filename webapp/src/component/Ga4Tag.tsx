import { useEffect } from 'react';
import TagManager from 'react-gtm-module';

import { useConfig } from 'tg.globalContext/helpers';

export const Ga4Tag = () => {
  const config = useConfig();
  const tag = config?.ga4Tag;

  useEffect(() => {
    if (tag) {
      TagManager.initialize({
        gtmId: tag,
      });
    }
  }, [tag]);

  return null;
};
