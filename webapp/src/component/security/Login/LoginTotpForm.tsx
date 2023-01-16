import React, { RefObject } from 'react';
import { Alert, Link as MuiLink, Typography } from '@mui/material';
import Box from '@mui/material/Box';
import { T, useTranslate } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { container } from 'tsyringe';

import { components } from 'tg.service/apiSchema.generated';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';

type LoginRequestDto = components['schemas']['LoginRequest'];

const globalActions = container.resolve(GlobalActions);

type Credentials = { username: string; password: string };
type LoginViewTotpProps = {
  credentialsRef: RefObject<Credentials>;
  onMfaCancel: () => void;
};

export function LoginTotpForm(props: LoginViewTotpProps) {
  const { t } = useTranslate();
  const security = useSelector((state: AppState) => state.global.security);
  const authLoading = useSelector(
    (state: AppState) => state.global.authLoading
  );

  return (
    <DashboardPage>
      <CompactView
        windowTitle={t('account-security-mfa')}
        title={t('account-security-mfa')}
        alerts={
          security.loginErrorCode &&
          !authLoading && (
            <Alert severity="error">
              <T>{security.loginErrorCode}</T>
            </Alert>
          )
        }
        content={
          <StandardForm
            initialValues={
              {
                username: props.credentialsRef.current!.username,
                password: props.credentialsRef.current!.password,
                otp: '',
              } as LoginRequestDto
            }
            submitButtons={
              <Box mt={2}>
                <Box display="flex" flexDirection="column" alignItems="stretch">
                  <LoadingButton
                    loading={authLoading}
                    variant="contained"
                    color="primary"
                    type="submit"
                    data-cy="login-button"
                  >
                    <T>login_login_button</T>
                  </LoadingButton>
                </Box>
              </Box>
            }
            onSubmit={(data) => globalActions.login.dispatch(data)}
          >
            <TextField
              name="otp"
              label={<T>account-security-mfa-otp-code</T>}
              variant="standard"
            />
          </StandardForm>
        }
        footer={
          <Box display="flex" justifyContent="flex-end">
            <MuiLink onClick={() => props.onMfaCancel()} component={'button'}>
              <Typography variant="caption">
                <T>global_cancel_button</T>
              </Typography>
            </MuiLink>
          </Box>
        }
      />
    </DashboardPage>
  );
}
