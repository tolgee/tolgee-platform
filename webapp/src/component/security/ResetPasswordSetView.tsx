import { FunctionComponent, useEffect } from 'react';
import { useTranslate } from '@tolgee/react';
import Box from '@mui/material/Box';
import { useSelector } from 'react-redux';
import { Redirect, useRouteMatch } from 'react-router-dom';
import { container } from 'tsyringe';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';
import { CompactView } from 'tg.component/layout/CompactView';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { Alert } from '../common/Alert';
import { StandardForm } from '../common/form/StandardForm';
import { DashboardPage } from '../layout/DashboardPage';
import { SetPasswordFields } from './SetPasswordFields';

const globalActions = container.resolve(GlobalActions);

type ValueType = {
  password: string;
  passwordRepeat: string;
};

const PasswordResetSetView: FunctionComponent = () => {
  const t = useTranslate();
  const match = useRouteMatch();
  const encodedData = match.params[PARAMS.ENCODED_EMAIL_AND_CODE];
  const [code, email] = atob(encodedData).split(',');

  useEffect(() => {
    globalActions.resetPasswordValidate.dispatch(email, code);
  }, []);

  const passwordResetSetLoading = useSelector(
    (state: AppState) => state.global.passwordResetSetLoading
  );
  const passwordResetSetError = useSelector(
    (state: AppState) => state.global.passwordResetSetError
  );
  const passwordResetSetValidated = useSelector(
    (state: AppState) => state.global.passwordResetSetValidated
  );
  const success = useSelector(
    (state: AppState) => state.global.passwordResetSetSucceed
  );

  const security = useSelector((state: AppState) => state.global.security);
  const remoteConfig = useConfig();

  if (
    !remoteConfig.authentication ||
    security.allowPrivate ||
    !remoteConfig.passwordResettable ||
    success
  ) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  return (
    <DashboardPage>
      {passwordResetSetValidated && (
        <CompactView
          alerts={
            passwordResetSetError && (
              <Alert severity="error">{passwordResetSetError}</Alert>
            )
          }
          windowTitle={t('reset_password_set_title')}
          title={t('reset_password_set_title')}
          content={
            <StandardForm
              initialValues={{ password: '', passwordRepeat: '' } as ValueType}
              validationSchema={Validation.USER_PASSWORD_WITH_REPEAT}
              submitButtons={
                <>
                  <Box display="flex">
                    <Box flexGrow={1}></Box>
                    <Box display="flex" flexGrow={0}>
                      <LoadingButton
                        color="primary"
                        type="submit"
                        variant="contained"
                        loading={passwordResetSetLoading}
                      >
                        Save new password
                      </LoadingButton>
                    </Box>
                  </Box>
                </>
              }
              //@ts-ignore
              onSubmit={(v: ValueType) => {
                globalActions.resetPasswordSet.dispatch(
                  email,
                  code,
                  v.password
                );
              }}
            >
              <SetPasswordFields />
            </StandardForm>
          }
        />
      )}
    </DashboardPage>
  );
};

export default PasswordResetSetView;
