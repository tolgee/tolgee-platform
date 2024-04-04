import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';

import { useUser } from 'tg.globalContext/helpers';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { StandardForm } from './common/form/StandardForm';
import { TextField } from './common/form/fields/TextField';
import { useLoadingRegister } from './GlobalLoading';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { components } from 'tg.service/apiSchema.generated';
type SuperTokenRequest = components['schemas']['SuperTokenRequest'];

export const SensitiveOperationAuthDialog = () => {
  const { superTokenRequestCancel, superTokenRequestSuccess } =
    useGlobalActions();
  const dialogOpen = useGlobalContext((c) => c.auth.superTokenNeeded);
  const user = useUser();

  const superTokenMutation = useApiMutation({
    url: '/v2/user/generate-super-token',
    method: 'post',

    fetchOptions: {
      disableAutoErrorHandle: true,
    },
  });

  // prevent loading indicator as original requests are pending in the background
  // but we are not interested in those
  useLoadingRegister(dialogOpen && !superTokenMutation.isLoading);

  const onCancel = () => {
    superTokenRequestCancel();
  };

  return (
    <Dialog open={dialogOpen} data-cy="sensitive-protection-dialog">
      <DialogTitle>
        <T keyName="sensitive-authentication-dialog-title" />
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2">
          <T keyName="sensitive-authentication-message" />
        </Typography>
        <StandardForm
          rootSx={{ mb: 0 }}
          submitButtonInner={<T keyName="sensitive-auth-submit-button" />}
          saveActionLoadable={superTokenMutation}
          onCancel={onCancel}
          initialValues={{ otp: '', password: '' } satisfies SuperTokenRequest}
          onSubmit={(values: SuperTokenRequest) => {
            superTokenMutation.mutate(
              { content: { 'application/json': values } },
              {
                onSuccess(res) {
                  superTokenRequestSuccess(res.accessToken!);
                },
              }
            );
          }}
        >
          <Box mb={2}>
            {user?.mfaEnabled ? (
              <>
                <Typography variant="body2">
                  <T keyName="sensitive-dialog-provide-2fa-code" />
                </Typography>
                <TextField
                  inputProps={{
                    'data-cy': 'sensitive-dialog-otp-input',
                  }}
                  name="otp"
                  label={<T keyName="account-security-mfa-otp-code" />}
                  variant="standard"
                />
              </>
            ) : (
              <TextField
                inputProps={{
                  'data-cy': 'sensitive-dialog-password-input',
                }}
                name="password"
                type="password"
                label={<T keyName="login_password_label" />}
                variant="standard"
              />
            )}
          </Box>
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
