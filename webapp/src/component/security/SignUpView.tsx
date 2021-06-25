import { default as React, FunctionComponent } from 'react';
import { Button } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import { T, useTranslate } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { Redirect } from 'react-router-dom';
import { container } from 'tsyringe';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.hooks/useConfig';
import { SignUpActions } from 'tg.store/global/SignUpActions';
import { AppState } from 'tg.store/index';

import { Alert } from '../common/Alert';
import { TextField } from '../common/form/fields/TextField';
import { BaseFormView } from '../layout/BaseFormView';
import { BaseView } from '../layout/BaseView';
import { DashboardPage } from '../layout/DashboardPage';
import { SetPasswordFields } from './SetPasswordFields';

const actions = container.resolve(SignUpActions);

export type SignUpType = {
  name: string;
  email: string;
  password: string;
  passwordRepeat?: string;
  invitationCode?: string;
};

const SignUpView: FunctionComponent = () => {
  const security = useSelector((state: AppState) => state.global.security);
  const state = useSelector((state: AppState) => state.signUp.loadables.signUp);
  const config = useConfig();
  const remoteConfig = useConfig();
  const t = useTranslate();

  if (
    !remoteConfig.authentication ||
    security.allowPrivate ||
    !security.allowRegistration
  ) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  return (
    <DashboardPage>
      {state.loaded && config.needsEmailVerification ? (
        <BaseView title={<T>sign_up_success_title</T>} lg={4} md={6} xs={12}>
          <Alert severity="success">
            <T>sign_up_success_needs_verification_message</T>
          </Alert>
        </BaseView>
      ) : (
        <BaseFormView
          loading={state.loading}
          title={<T>sign_up_title</T>}
          lg={4}
          md={6}
          xs={12}
          saveActionLoadable={state}
          initialValues={
            {
              password: '',
              passwordRepeat: '',
              name: '',
              email: '',
            } as SignUpType
          }
          validationSchema={Validation.SIGN_UP(t)}
          submitButtons={
            <Box display="flex" justifyContent="flex-end">
              <Button color="primary" type="submit">
                <T>sign_up_submit_button</T>
              </Button>
            </Box>
          }
          onSubmit={(v: SignUpType) => {
            actions.loadableActions.signUp.dispatch(v);
          }}
        >
          <TextField name="name" label={<T>sign_up_form_full_name</T>} />
          <TextField name="email" label={<T>sign_up_form_email</T>} />
          <SetPasswordFields />
        </BaseFormView>
      )}
    </DashboardPage>
  );
};

export default SignUpView;
