import React, { FunctionComponent, useEffect } from 'react';
import { Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';

import { components } from 'tg.service/apiSchema.generated';
import { SecurityService } from 'tg.service/SecurityService';

import { StandardForm } from 'tg.component/common/form/StandardForm';
import { LINKS } from 'tg.constants/links';
import { redirect } from 'tg.hooks/redirect';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useUser } from 'tg.globalContext/helpers';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { Validation } from 'tg.constants/GlobalValidationSchema';

type TotpDisableDto = components['schemas']['UserTotpDisableRequestDto'];

const securityService = container.resolve(SecurityService);

export const DisableMfaDialog: FunctionComponent = () => {
  const onDialogClose = () => {
    redirect(LINKS.USER_ACCOUNT_SECURITY);
  };

  const user = useUser();
  const message = useMessage();
  const { t } = useTranslate();
  const { refetchInitialData } = useGlobalActions();

  useEffect(() => {
    if (user && !user.mfaEnabled) onDialogClose();
  }, [user]);

  const enableMfa = useApiMutation({
    url: '/v2/user/mfa/totp',
    method: 'delete',
    options: {
      onSuccess: (r) => {
        securityService.setToken(r.accessToken!);
        message.success(<T keyName="account-security-mfa-disabled-success" />);
        refetchInitialData();
        onDialogClose();
      },
    },
  });

  if (!user) return null;

  return (
    <Dialog
      open={true}
      onClose={onDialogClose}
      fullWidth
      maxWidth="sm"
      data-cy="mfa-disable-dialog"
    >
      <DialogTitle data-cy="mfa-disable-dialog-title">
        <T>account-security-mfa-disable-mfa</T>
      </DialogTitle>
      <DialogContent data-cy="mfa-disable-dialog-content">
        <StandardForm
          onSubmit={(values) => {
            enableMfa.mutate({
              content: { 'application/json': values },
            });
          }}
          saveActionLoadable={enableMfa}
          onCancel={() => onDialogClose()}
          initialValues={
            {
              password: '',
            } as TotpDisableDto
          }
          submitButtonInner={t('account-security-mfa-disable-mfa-button')}
          validationSchema={Validation.USER_MFA_DISABLE}
        >
          <TextField
            inputProps={{
              'data-cy': 'mfa-disable-dialog-password-input',
            }}
            name="password"
            type="password"
            label={t('Password')}
          />
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
