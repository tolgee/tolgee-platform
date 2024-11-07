import { FunctionComponent, useEffect } from 'react';

import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { useSsoService } from 'tg.component/security/SsoService';

interface SsoRedirectionHandlerProps {}

export const SsoRedirectionHandler: FunctionComponent<
  SsoRedirectionHandlerProps
> = () => {
  const allowPrivate = useGlobalContext((c) => c.auth.allowPrivate);

  const { login } = useSsoService();

  useEffect(() => {
    const searchParam = new URLSearchParams(window.location.search);
    const code = searchParam.get('code');
    const state = searchParam.get('state');

    if (code && state && !allowPrivate) {
      login(state, code);
    }
  }, [allowPrivate]);

  return <FullPageLoading />;
};
