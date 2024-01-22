import { default as React, FunctionComponent } from 'react';
import { useSelector } from 'react-redux';
import { Redirect, Route } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';
import { securityService } from 'tg.service/SecurityService';
import { AppState } from 'tg.store/index';

interface PrivateRouteProps {}

export const PrivateRoute: FunctionComponent<
  PrivateRouteProps & React.ComponentProps<typeof Route>
> = (props) => {
  const allowPrivate = useSelector(
    (state: AppState) => state.global.security.allowPrivate
  );
  const afterLoginLink = securityService.getAfterLoginLink();

  if (allowPrivate && afterLoginLink) {
    securityService.removeAfterLoginLink();
    return <Redirect to={afterLoginLink} />;
  }

  if (allowPrivate) {
    return <Route {...props} />;
  }

  return (
    <Route
      render={({ location }) => (
        <Redirect
          to={{
            pathname: LINKS.LOGIN.build(),
            state: { from: location },
          }}
        />
      )}
    />
  );
};
