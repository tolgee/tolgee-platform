import { FunctionComponent, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Redirect, useRouteMatch } from 'react-router-dom';
import { container } from 'tsyringe';

import { LINKS, PARAMS } from 'tg.constants/links';
import { GlobalActions, GlobalState } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';

import { FullPageLoading } from 'tg.component/common/FullPageLoading';

interface OAuthRedirectionHandlerProps {}

const actions = container.resolve(GlobalActions);

export const OAuthRedirectionHandler: FunctionComponent<
  OAuthRedirectionHandlerProps
> = (props) => {
  const security = useSelector<AppState, GlobalState['security']>(
    (state) => state.global.security
  );

  const match = useRouteMatch();

  useEffect(() => {
    const code = new URLSearchParams(window.location.search).get('code');
    if (code && !security.allowPrivate) {
      actions.oAuthSuccessful.dispatch(match.params[PARAMS.SERVICE_TYPE], code);
    }
  }, [security.allowPrivate]);

  if (security.jwtToken) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  if (security.loginErrorCode) {
    return <Redirect to={LINKS.LOGIN.build()} />;
  }

  return (
    <>
      <FullPageLoading />
    </>
  );
};
