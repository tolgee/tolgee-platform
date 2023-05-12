import { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { gtag, install } from 'ga-gtag';

import { useConfig } from 'tg.globalContext/helpers';
import { AppState } from 'tg.store/index';

export const loadScript = (tagId: string) => {
  /* START - COPIED FROM ANALYTICS INSTRUCTIONS */
  // @ts-ignore
  install(tagId);

  gtag('js', new Date());

  gtag('config', tagId);
  /* END - COPIED FROM ANALYTICS INSTRUCTIONS */
};

export const Ga4Tag = () => {
  const config = useConfig();
  const tag = config?.ga4Tag;

  const allowPrivate = useSelector(
    (state: AppState) => state.global.security.allowPrivate
  );

  useEffect(() => {
    if (tag && allowPrivate) {
      loadScript(tag!);
    }
  }, [tag, allowPrivate]);

  return null;
};
