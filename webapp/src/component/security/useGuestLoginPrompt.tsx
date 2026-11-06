import { Link, useHistory } from 'react-router-dom';
import { Link as MuiLink } from '@mui/material';
import { T } from '@tolgee/react';

import { LINKS } from 'tg.constants/links';
import { confirmation } from 'tg.hooks/confirmation';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useRegistrationAllowed } from 'tg.globalContext/helpers';

export const useGuestLoginPrompt = () => {
  const { rememberCurrentLocation, closeConfirmation } = useGlobalActions();
  const history = useHistory();
  const registrationAllowed = useRegistrationAllowed();

  return () =>
    confirmation({
      title: (
        <T
          keyName="community_guest_login_dialog_title"
          defaultValue="Log in to suggest translations"
        />
      ),
      message: registrationAllowed ? (
        <T
          keyName="community_guest_login_dialog_message"
          defaultValue="Suggesting translations needs an account. Log in, or <signUp>sign up</signUp> if you don't have one yet."
          params={{
            signUp: (
              <MuiLink
                component={Link}
                to={LINKS.SIGN_UP.build()}
                data-cy="guest-login-dialog-sign-up-link"
                onClick={() => {
                  rememberCurrentLocation();
                  closeConfirmation();
                }}
              />
            ),
          }}
        />
      ) : (
        <T
          keyName="community_guest_login_dialog_message_no_sign_up"
          defaultValue="Suggesting translations needs an account. Log in to continue."
        />
      ),
      confirmButtonText: <T keyName="public_projects_login_button" />,
      onConfirm: () => {
        rememberCurrentLocation();
        history.push(LINKS.LOGIN.build());
      },
    });
};
