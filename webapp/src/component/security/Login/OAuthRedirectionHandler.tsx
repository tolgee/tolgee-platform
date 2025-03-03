import { FunctionComponent, useEffect } from 'react';
import { Redirect, useHistory, useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';

import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';

interface OAuthRedirectionHandlerProps {}
export const OAuthRedirectionHandler: FunctionComponent<
  OAuthRedirectionHandlerProps
> = () => {
  const loginLoadable = useGlobalContext((c) => c.auth.authorizeOAuthLoadable);

  const {
    loginWithOAuthCode,
    getLastSsoDomain,
    getOAuthStateKey,
    getSsoStateKey,
    clearOAuthStateKey,
    clearSsoStateKey,
  } = useGlobalActions();
  const match = useRouteMatch();
  const history = useHistory();

  useEffect(() => {
    const url = new URLSearchParams(window.location.search);
    const type = match.params[PARAMS.SERVICE_TYPE];
    const code = url.get('code');
    let domain: string | undefined = undefined;

    function checkState(
      storedState: string | undefined,
      clearfn: () => void
    ): boolean {
      const state = url.get('state');
      if (storedState !== state) {
        history.replace(LINKS.LOGIN.build());
        return false;
      } else {
        clearfn();
        return true;
      }
    }

    if (
      type == 'oauth2' &&
      !checkState(getOAuthStateKey(), clearOAuthStateKey)
    ) {
      return;
    }

    if (type == 'sso') {
      if (!checkState(getSsoStateKey(), clearSsoStateKey)) {
        return;
      }

      domain = getLastSsoDomain();
      if (!domain) {
        history.replace(LINKS.LOGIN.build());
        return;
      }
    }

    if (!code) {
      history.replace(LINKS.LOGIN.build());
      return;
    }

    loginWithOAuthCode(type, code, domain);
  }, []);

  if (loginLoadable.error) {
    return (
      <Redirect to={LINKS.LOGIN.build()}>
        <FullPageLoading />
      </Redirect>
    );
  }

  return <FullPageLoading />;
};
