import { useSelector } from 'react-redux';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';

import { AppState } from 'tg.store/index';
import { useUser } from 'tg.globalContext/helpers';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { globalActions } from 'tg.store/global/GlobalActions';
import { StandardForm } from './common/form/StandardForm';
import { TextField } from './common/form/fields/TextField';
import { useLoadingRegister } from './GlobalLoading';

type Value = { otp?: string; password?: string };

export const SensitiveOperationAuthDialog = () => {
  const afterActions = useSelector(
    (s: AppState) => s.global.requestSuperJwtAfterActions
  );
  const user = useUser();

  const dialogOpen = afterActions.length > 0;

  const superTokenMutation = useApiMutation({
    url: '/v2/user/generate-super-token',
    method: 'post',
    options: {
      onSuccess(res) {
        const onSuccessCallbacks = afterActions.map(
          (action) => action.onSuccess
        );
        globalActions.successSuperJwtRequest.dispatch(res.accessToken!);
        onSuccessCallbacks.forEach((fn) => fn());
      },
    },
    fetchOptions: {
      disableAutoErrorHandle: true,
    },
  });

  // prevent loading indicator as original requests are pending in the background
  // but we are not interested in those
  useLoadingRegister(dialogOpen && !superTokenMutation.isLoading);

  const onCancel = () => {
    afterActions.forEach((action) => {
      action.onCancel();
    });
    globalActions.cancelSuperJwtRequest.dispatch();
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
          initialValues={{ otp: '', password: '' } as Value}
          onSubmit={(v: Value) => {
            superTokenMutation.mutate({ content: { 'application/json': v } });
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
