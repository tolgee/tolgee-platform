import { Button } from '@mui/material';
import { Link } from 'react-router-dom';
import { T } from '@tolgee/react';

import { LINKS } from 'tg.constants/links';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useRegistrationAllowed } from 'tg.globalContext/helpers';

export const LoginSignUpButtons = () => {
  const { saveAfterLoginLink, currentLocation } = useGlobalActions();
  const rememberLocation = () => saveAfterLoginLink(currentLocation());
  const registrationAllowed = useRegistrationAllowed();

  return (
    <>
      <Button
        component={Link}
        to={LINKS.LOGIN.build()}
        onClick={rememberLocation}
        variant="outlined"
        size="small"
        data-cy="guest-login-button"
      >
        <T keyName="public_projects_login_button" />
      </Button>
      {registrationAllowed && (
        <Button
          component={Link}
          to={LINKS.SIGN_UP.build()}
          onClick={rememberLocation}
          variant="contained"
          color="primary"
          size="small"
          sx={{ ml: 1 }}
          data-cy="guest-sign-up-button"
        >
          <T keyName="public_projects_sign_up_button" />
        </Button>
      )}
    </>
  );
};
