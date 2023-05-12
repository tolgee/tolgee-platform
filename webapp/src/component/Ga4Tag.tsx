import { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { gtag, install } from 'ga-gtag';

import { useConfig } from 'tg.globalContext/helpers';
import { AppState } from 'tg.store/index';

export const initGtag = (tagId: string) => {
  install(tagId);
  gtag('js', new Date());
  gtag('config', tagId);
};

export const Ga4Tag = () => {
  const config = useConfig();
  const tag = config?.ga4Tag;

  const allowPrivate = useSelector(
    (state: AppState) => state.global.security.allowPrivate
  );

  useEffect(() => {
    if (tag && allowPrivate) {
      initGtag(tag!);
    }
  }, [tag, allowPrivate]);

  return null;
};
