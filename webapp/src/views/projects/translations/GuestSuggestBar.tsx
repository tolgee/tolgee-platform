import { Alert } from '@mui/material';
import { T } from '@tolgee/react';

import { useRegistrationAllowed } from 'tg.globalContext/helpers';

export const GuestSuggestBar = () => {
  const registrationAllowed = useRegistrationAllowed();
  return (
    <Alert severity="info" sx={{ mb: 1 }} data-cy="translations-guest-bar">
      {registrationAllowed ? (
        <T
          keyName="community_guest_translations_bar"
          defaultValue="You are browsing as a guest. Log in or sign up to suggest translations."
        />
      ) : (
        <T
          keyName="community_guest_translations_bar_no_sign_up"
          defaultValue="You are browsing as a guest. Log in to suggest translations."
        />
      )}
    </Alert>
  );
};
