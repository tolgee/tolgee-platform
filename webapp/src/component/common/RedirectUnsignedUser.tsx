import { useEffect } from 'react';
import { Redirect, useHistory } from 'react-router-dom';
import { GlobalLoading } from 'tg.component/GlobalLoading';
import { LINKS } from 'tg.constants/links';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useIsGuest } from 'tg.globalContext/helpers';

export const RedirectUnsignedUser: React.FC<
  React.PropsWithChildren<unknown>
> = ({ children }) => {
  const shouldRedirect = useIsGuest();

  const history = useHistory();
  const currentLocation = history.location.pathname + history.location.search;
  const { rememberCurrentLocation } = useGlobalActions();

  useEffect(() => {
    if (shouldRedirect) {
      rememberCurrentLocation(currentLocation);
    }
  }, [shouldRedirect, currentLocation]);

  if (shouldRedirect) {
    return (
      <Redirect to={LINKS.LOGIN.build()}>
        <GlobalLoading />
      </Redirect>
    );
  } else {
    return <>{children}</>;
  }
};
