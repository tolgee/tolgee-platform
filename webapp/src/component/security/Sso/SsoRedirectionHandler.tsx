import { FunctionComponent, useEffect } from 'react';

import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';

interface SsoRedirectionHandlerProps {}

export const SsoRedirectionHandler: FunctionComponent<
  SsoRedirectionHandlerProps
> = () => {
  const allowPrivate = useGlobalContext((c) => c.auth.allowPrivate);
  const { loginWithOAuthCodeSso } = useGlobalActions();

  useEffect(() => {
    const searchParam = new URLSearchParams(window.location.search);
    const code = searchParam.get('code');
    const state = searchParam.get('state');

    if (code && state && !allowPrivate) {
      loginWithOAuthCodeSso(state, code);
    }
  }, [allowPrivate]);

  return <FullPageLoading />;
};
