import { container } from 'tsyringe';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';
import { useUser } from 'tg.globalContext/helpers';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';
import { StandardForm } from './common/form/StandardForm';
import { TextField } from './common/form/fields/TextField';
import React from 'react';

type Value = { otp?: string; password?: string };

export const SensitiveOperationAuthDialog = () => {
  const globalActions = container.resolve(GlobalActions);
  const afterActions = useSelector(
    (s: AppState) => s.global.requestSuperJwtAfterActions
  );
  const user = useUser();

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
      disableAuthHandling: true,
    },
  });

  const onCancel = () => {
    afterActions.forEach((action) => {
      action.onCancel();
    });
    globalActions.cancelSuperJwtRequest.dispatch();
  };

  return (
    <Dialog
      open={afterActions.length > 0}
      data-cy="sensitive-protection-dialog"
    >
      <DialogTitle>
        <T keyName="sensitive-authentication-dialog-title" />
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2">
          <T keyName="sensitive-authentication-message" />
        </Typography>
        <StandardForm
          rootSx={{ mb: 0 }}
          submitButtonInner={<T>sensitive-auth-submit-button</T>}
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
                  label={<T>account-security-mfa-otp-code</T>}
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
                label={<T>login_password_label</T>}
                variant="standard"
              />
            )}
          </Box>
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
