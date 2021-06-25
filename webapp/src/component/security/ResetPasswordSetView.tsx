import { default as React, FunctionComponent, useEffect } from 'react';
import { Button } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import { useSelector } from 'react-redux';
import { Redirect, useRouteMatch } from 'react-router-dom';
import { container } from 'tsyringe';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useConfig } from 'tg.hooks/useConfig';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';

import { Alert } from '../common/Alert';
import { StandardForm } from '../common/form/StandardForm';
import { BaseView } from '../layout/BaseView';
import { DashboardPage } from '../layout/DashboardPage';
import { SetPasswordFields } from './SetPasswordFields';

const globalActions = container.resolve(GlobalActions);

type ValueType = {
  password: string;
  passwordRepeat: string;
};

const PasswordResetSetView: FunctionComponent = () => {
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
      <BaseView lg={6} md={8} xs={12} loading={passwordResetSetLoading}>
        {passwordResetSetError && (
          <Box mt={1}>
            <Alert severity="error">{passwordResetSetError}</Alert>
          </Box>
        )}
        {passwordResetSetValidated && (
          <StandardForm
            initialValues={{ password: '', passwordRepeat: '' } as ValueType}
            validationSchema={Validation.USER_PASSWORD_WITH_REPEAT}
            submitButtons={
              <>
                <Box display="flex">
                  <Box flexGrow={1}></Box>
                  <Box display="flex" flexGrow={0}>
                    <Button color="primary" type="submit">
                      Save new password
                    </Button>
                  </Box>
                </Box>
              </>
            }
            //@ts-ignore
            onSubmit={(v: ValueType) => {
              globalActions.resetPasswordSet.dispatch(email, code, v.password);
            }}
          >
            <SetPasswordFields />
          </StandardForm>
        )}
      </BaseView>
    </DashboardPage>
  );
};

export default PasswordResetSetView;
