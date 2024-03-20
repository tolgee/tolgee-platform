import { RefObject } from 'react';
import { Alert } from '@mui/material';
import Box from '@mui/material/Box';
import { T, useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';

type LoginRequestDto = components['schemas']['LoginRequest'];

type Credentials = { username: string; password: string };
type LoginViewTotpProps = {
  credentialsRef: RefObject<Credentials>;
  onMfaCancel: () => void;
};

export function LoginTotpForm(props: LoginViewTotpProps) {
  const { t } = useTranslate();
  const { login } = useGlobalActions();
  const loginErrorCode = useGlobalContext(
    (c) => c.auth.loginLoadable.error?.code
  );

  const authLoading = useGlobalContext((c) => c.auth.loginLoadable.isLoading);

  return (
    <DashboardPage>
      <CompactView
        windowTitle={t('account-security-mfa')}
        title={t('account-security-mfa')}
        backLink={() => props.onMfaCancel()}
        alerts={
          loginErrorCode &&
          loginErrorCode !== 'mfa_enabled' &&
          !authLoading && (
            <Alert severity="error">
              <TranslatedError code={loginErrorCode} />
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
            onSubmit={(data) => login(data)}
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
