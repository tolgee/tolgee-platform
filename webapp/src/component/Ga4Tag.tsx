import { useEffect } from 'react';
import { useSelector } from 'react-redux';

import { useConfig } from 'tg.globalContext/helpers';
import { AppState } from 'tg.store/index';

const BASE_URL = 'https://www.googletagmanager.com/gtag/js?id=';
let scriptPromise: Promise<void> | null = null;

export const loadScript = (tagId: string) => {
  if (scriptPromise) {
    return scriptPromise;
  }
  const scriptElement = document.createElement('script') as HTMLScriptElement;

  scriptElement.src = `${BASE_URL}${tagId}`;
  scriptElement.defer = true;
  scriptElement.async = true;

  document.head.appendChild(scriptElement);

  scriptPromise = new Promise<void>((resolve) => {
    scriptElement.onload = function () {
      resolve();
    };
  });

  /* START - COPIED FROM ANALYTICS INSTRUCTIONS */
  // @ts-ignore
  window.dataLayer = window.dataLayer || [];
  function gtag(...args): void {
    // @ts-ignore
    dataLayer.push(args);
  }
  gtag('js', new Date());

  gtag('config', tagId);
  /* END - COPIED FROM ANALYTICS INSTRUCTIONS */

  return scriptPromise;
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
