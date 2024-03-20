import { FunctionComponent } from 'react';
import Box from '@mui/material/Box';
import { T, useTranslate } from '@tolgee/react';
import { Redirect } from 'react-router-dom';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { CompactView } from 'tg.component/layout/CompactView';

import { Alert } from '../common/Alert';
import { StandardForm } from '../common/form/StandardForm';
import { TextField } from '../common/form/fields/TextField';
import { DashboardPage } from '../layout/DashboardPage';
import { GlobalLoading } from 'tg.component/GlobalLoading';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

interface LoginProps {}

type ValueType = {
  email: string;
};

const PasswordResetView: FunctionComponent<LoginProps> = () => {
  const { t } = useTranslate();
  const remoteConfig = useConfig();

  const { error, isSuccess, isLoading, mutate } = useApiMutation({
    url: '/api/public/reset_password_request',
    method: 'post',
  });

  if (!remoteConfig.passwordResettable) {
    return (
      <Redirect to={LINKS.LOGIN.build()}>
        <GlobalLoading />
      </Redirect>
    );
  }

  return (
    <DashboardPage>
      <CompactView
        alerts={
          error?.code &&
          !isLoading && (
            <Alert severity="error">
              <TranslatedError code={error.code} />
            </Alert>
          )
        }
        windowTitle={t('reset_password_title')}
        title={t('reset_password_title')}
        backLink={LINKS.LOGIN.build()}
        maxWidth={650}
        content={
          isSuccess ? (
            <Alert severity="success">
              <T keyName="reset_password_success_message" />
            </Alert>
          ) : (
            <StandardForm
              initialValues={{ email: '' } as ValueType}
              validationSchema={Validation.RESET_PASSWORD_REQUEST}
              submitButtons={
                <>
                  <Box display="flex">
                    <Box flexGrow={1}></Box>
                    <Box display="flex" flexGrow={0}>
                      <LoadingButton
                        color="primary"
                        type="submit"
                        variant="contained"
                        loading={isLoading}
                      >
                        <T keyName="reset_password_send_request_button" />
                      </LoadingButton>
                    </Box>
                  </Box>
                </>
              }
              onSubmit={(v: ValueType) => {
                mutate({
                  content: {
                    'application/json': {
                      email: v.email,
                      callbackUrl: LINKS.RESET_PASSWORD.buildWithOrigin(),
                    },
                  },
                });
              }}
            >
              <TextField
                name="email"
                label={<T keyName="reset_password_email_field" />}
                variant="standard"
              />
            </StandardForm>
          )
        }
      />
    </DashboardPage>
  );
};

export default PasswordResetView;
