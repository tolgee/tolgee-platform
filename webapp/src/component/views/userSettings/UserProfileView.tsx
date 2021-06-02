import { default as React, FunctionComponent, useEffect } from 'react';
import { container } from 'tsyringe';

import { useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';

import { T } from '@tolgee/react';
import { AppState } from '../../../store';
import { Validation } from '../../../constants/GlobalValidationSchema';
import { SetPasswordFields } from '../../security/SetPasswordFields';
import { UserActions } from '../../../store/global/UserActions';
import { UserUpdateDTO } from '../../../service/request.types';
import { TextField } from '../../common/form/fields/TextField';
import { BaseUserSettingsView } from './BaseUserSettingsView';
import { StandardForm } from '../../common/form/StandardForm';
import { useFormikContext } from 'formik';
import { Box, Typography } from '@material-ui/core';
import { useConfig } from '../../../hooks/useConfig';

const actions = container.resolve(UserActions);
const userActions = container.resolve(UserActions);

export const UserProfileView: FunctionComponent = () => {
  let saveLoadable = useSelector(
    (state: AppState) => state.user.loadables.updateUser
  );
  let resourceLoadable = useSelector(
    (state: AppState) => state.user.loadables.userData
  );

  useEffect(() => {
    if (saveLoadable.loaded) {
      userActions.loadableActions.userData.dispatch();
    }
  }, [saveLoadable.loading]);

  const history = useHistory();
  const config = useConfig();

  const Fields = () => {
    const formik = useFormikContext();
    const initialEmail = formik.getFieldMeta('email').initialValue;
    const newEmail = formik.getFieldMeta('email').value;
    const emailChanged = newEmail !== initialEmail;

    return (
      <>
        <TextField name="name" label={<T>User settings - Full name</T>} />
        <TextField name="email" label={<T>User settings - E-mail</T>} />
        {resourceLoadable?.data?.emailAwaitingVerification && (
          <Box>
            <Typography variant="body1">
              <T
                parameters={{
                  email: resourceLoadable.data.emailAwaitingVerification!,
                }}
              >
                email_waiting_for_verification
              </T>
            </Typography>
          </Box>
        )}

        {emailChanged && config.needsEmailVerification && (
          <Typography variant="body1">
            <T>your_email_was_changed_verification_message</T>
          </Typography>
        )}
        <SetPasswordFields />
      </>
    );
  };

  return (
    <BaseUserSettingsView
      title={<T>user_profile_title</T>}
      loading={resourceLoadable.loading}
    >
      <StandardForm
        saveActionLoadable={saveLoadable}
        initialValues={
          {
            password: '',
            passwordRepeat: '',
            name: resourceLoadable.data!.name,
            email: resourceLoadable.data!.username,
          } as UserUpdateDTO
        }
        validationSchema={Validation.USER_SETTINGS}
        onCancel={() => history.goBack()}
        onSubmit={(v: UserUpdateDTO) => {
          if (!v.password) {
            delete v.password;
          }
          actions.loadableActions.updateUser.dispatch(v);
        }}
      >
        <Fields />
      </StandardForm>
    </BaseUserSettingsView>
  );
};
