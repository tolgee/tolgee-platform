import {FunctionComponent, useEffect} from 'react';
import {useHistory} from 'react-router-dom';

import {LINKS} from 'tg.constants/links';

import {useGlobalContext} from 'tg.globalContext/GlobalContext';
import {FullPageLoading} from 'tg.component/common/FullPageLoading';
import {useSsoService} from 'tg.component/security/SsoService';

interface SsoRedirectionHandlerProps {}
const LOCAL_STORAGE_STATE_KEY = 'oauth2State';
const LOCAL_STORAGE_DOMAIN_KEY = 'oauth2Domain';

export const SsoRedirectionHandler: FunctionComponent<
  SsoRedirectionHandlerProps
> = () => {
  const allowPrivate = useGlobalContext((c) => c.auth.allowPrivate);

  const { loginWithOAuthCodeOpenId } = useSsoService();
  const history = useHistory();

  useEffect(() => {
    const searchParam = new URLSearchParams(window.location.search);
    const code = searchParam.get('code');
    const state = searchParam.get('state');

    const storedState = localStorage.getItem(LOCAL_STORAGE_STATE_KEY);
    const storedDomain = localStorage.getItem(LOCAL_STORAGE_DOMAIN_KEY);
    if (storedState !== state) {
      history.replace(LINKS.LOGIN.build());
    } else {
      localStorage.removeItem(LOCAL_STORAGE_STATE_KEY);
    }

    if (code && !allowPrivate && storedDomain) {
      loginWithOAuthCodeOpenId(storedDomain, code);
    }
  }, [allowPrivate]);

  return <FullPageLoading />;
};
