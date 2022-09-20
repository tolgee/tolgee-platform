import { container } from 'tsyringe';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';
import { useUser } from 'tg.globalContext/helpers';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T } from '@tolgee/react';
import { StandardForm } from './common/form/StandardForm';
import { TextField } from './common/form/fields/TextField';
import React from 'react';

type Value = { otp?: string; password?: string };

export const SuperJwtDialog = () => {
  const globalActions = container.resolve(GlobalActions);
  const afterAction = useSelector(
    (s: AppState) => s.global.requestSuperJwtAfterAction
  );
  const user = useUser();

  const superTokenMutation = useApiMutation({
    url: '/v2/user/generate-super-token',
    method: 'post',
    options: {
      onSuccess(res) {
        const onSuccess = afterAction?.onSuccess;
        globalActions.successSuperJwtRequest.dispatch(res.accessToken!);
        onSuccess?.();
      },
    },
  });

  return (
    <Dialog open={!!afterAction}>
      <DialogTitle>
        <T keyName="sensitive-authentication-dialog-title" />
      </DialogTitle>
      <DialogContent>
        <StandardForm
          saveActionLoadable={superTokenMutation}
          onCancel={() => {
            globalActions.cancelSuperJwtRequest.dispatch();
          }}
          initialValues={{ otp: '', password: '' } as Value}
          onSubmit={(v: Value) => {
            superTokenMutation.mutate({ content: { 'application/json': v } });
          }}
        >
          {user?.mfaEnabled ? (
            <TextField
              name="otp"
              label={<T>account-security-mfa-otp-code</T>}
              variant="standard"
            />
          ) : (
            <TextField
              name="password"
              type="password"
              label={<T>login_password_label</T>}
              variant="standard"
            />
          )}
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
