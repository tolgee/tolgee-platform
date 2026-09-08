import { Button } from '@mui/material';
import { T } from '@tolgee/react';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';

export function RevokeAllOtherSessionsButton() {
  const message = useMessage();

  const revokeAllMutation = useApiMutation({
    url: '/v2/user/sessions/other',
    method: 'delete',
    invalidatePrefix: ['/v2/user/sessions', '/v2/user/connected-apps'],
    options: {
      onSuccess: () =>
        message.success(
          <T
            keyName="sessions-revoked-all-with-apps-message"
            defaultValue="All other sessions were revoked and all apps disconnected"
          />
        ),
    },
  });

  const onRevokeAll = () => {
    confirmation({
      confirmButtonText: (
        <T
          keyName="sessions-revoke-all-others-button"
          defaultValue="Revoke all other sessions"
        />
      ),
      message: (
        <T
          keyName="sessions-revoke-all-others-confirmation-with-apps"
          defaultValue="Do you really want to revoke all sessions except the one you are using right now, and disconnect every app connected to your account? All other devices will be logged out."
        />
      ),
      onConfirm: () => revokeAllMutation.mutate({}),
    });
  };

  return (
    <Button
      data-cy="sessions-revoke-all-others-button"
      variant="outlined"
      color="error"
      onClick={onRevokeAll}
    >
      <T
        keyName="sessions-revoke-all-others-button"
        defaultValue="Revoke all other sessions"
      />
    </Button>
  );
}
