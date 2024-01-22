import { RefObject } from 'react';
import { Alert } from '@mui/material';
import Box from '@mui/material/Box';
import { T, useTranslate } from '@tolgee/react';
import { useSelector } from 'react-redux';

import { components } from 'tg.service/apiSchema.generated';
import { AppState } from 'tg.store/index';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { globalActions } from 'tg.store/global/GlobalActions';

type LoginRequestDto = components['schemas']['LoginRequest'];

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
        backLink={() => props.onMfaCancel()}
        alerts={
          security.loginErrorCode &&
          !authLoading && (
            <Alert severity="error">
              <TranslatedError code={security.loginErrorCode} />
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
                    <T keyName="login_login_button" />
                  </LoadingButton>
                </Box>
              </Box>
            }
            onSubmit={(data) => globalActions.login.dispatch(data)}
          >
            <TextField
              name="otp"
              label={<T keyName="account-security-mfa-otp-code" />}
              variant="standard"
            />
          </StandardForm>
        }
      />
    </DashboardPage>
  );
}
