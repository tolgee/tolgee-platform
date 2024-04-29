import { useMemo } from 'react';
import { Redirect, useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { securityService } from 'tg.service/SecurityService';

function rememberCurrentLocation() {
  const currentUrl = window.location.pathname + window.location.search;
  if (currentUrl !== '/' && currentUrl !== '/login') {
    securityService.saveAfterLoginLink({
      url: currentUrl,
    });
  }
}

// use to make user sign up first and then continue to current url
export const useAfterLoginRedirect = () => {
  const history = useHistory();
  return () => {
    rememberCurrentLocation();
    history.replace(LINKS.LOGIN.build());
  };
};

export const AfterLoginRedirect = () => {
  useMemo(() => rememberCurrentLocation(), []);
  return <Redirect to={LINKS.LOGIN.build()} />;
};
