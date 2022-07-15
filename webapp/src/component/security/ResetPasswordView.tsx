import { FunctionComponent, useEffect } from 'react';
import Box from '@mui/material/Box';
import { T, useTranslate } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { Redirect } from 'react-router-dom';
import { container } from 'tsyringe';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';

import { Alert } from '../common/Alert';
import { StandardForm } from '../common/form/StandardForm';
import { TextField } from '../common/form/fields/TextField';
import { DashboardPage } from '../layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import LoadingButton from 'tg.component/common/form/LoadingButton';

interface LoginProps {}

const globalActions = container.resolve(GlobalActions);

type ValueType = {
  email: string;
};

const PasswordResetView: FunctionComponent<LoginProps> = (props) => {
  const t = useTranslate();
  const security = useSelector((state: AppState) => state.global.security);
  const remoteConfig = useConfig();

  const loadable = useSelector(
    (state: AppState) => state.global.loadables.resetPasswordRequest
  );

  if (
    !remoteConfig.authentication ||
    security.allowPrivate ||
    !remoteConfig.passwordResettable
  ) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  useEffect(
    () => () => globalActions.loadableReset.resetPasswordRequest.dispatch(),
    []
  );

  return (
    <DashboardPage>
      <CompactView
        alerts={
          loadable.error && <Alert severity="error">{loadable.error}</Alert>
        }
        windowTitle={t('reset_password_title')}
        title={t('reset_password_title')}
        backLink={LINKS.LOGIN.build()}
        content={
          loadable.loaded ? (
            <Alert severity="success">
              <T>reset_password_success_message</T>
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
                        loading={loadable.loading}
                      >
                        <T>reset_password_send_request_button</T>
                      </LoadingButton>
                    </Box>
                  </Box>
                </>
              }
              // @ts-ignore
              onSubmit={(v: ValueType) => {
                globalActions.loadableActions.resetPasswordRequest.dispatch(
                  v.email
                );
              }}
            >
              <TextField
                name="email"
                label={<T>reset_password_email_field</T>}
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
