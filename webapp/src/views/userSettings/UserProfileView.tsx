import { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';
import { useHistory } from 'react-router-dom';
import { container } from 'tsyringe';

import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { SetPasswordFields } from 'tg.component/security/SetPasswordFields';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useConfig } from 'tg.hooks/useConfig';
import { MessageService } from 'tg.service/MessageService';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { UserUpdateDTO } from 'tg.service/request.types';
import { BaseSettingsView } from '../../component/layout/BaseSettingsView';
import { UserProfileAvatar } from './UserProfileAvatar';

const messagesService = container.resolve(MessageService);

export const UserProfileView: FunctionComponent = () => {
  const t = useTranslate();
  const userLoadable = useApiQuery({
    url: '/api/user',
    method: 'get',
    options: {
      cacheTime: 0,
    },
  });

  const updateUser = useApiMutation({
    url: '/api/user',
    method: 'post',
  });

  const handleSubmit = (v: UserUpdateDTO) => {
    if (!v.password) {
      delete v.password;
    }
    // @ts-ignore
    v.callbackUrl = window.location.protocol + '//' + window.location.host;
    updateUser.mutate(
      { content: { 'application/json': v } },
      {
        onSuccess() {
          messagesService.success(<T>User data - Successfully updated!</T>);
          userLoadable.refetch();
        },
      }
    );
  };

  const history = useHistory();
  const config = useConfig();

  const Fields = () => {
    const formik = useFormikContext();
    const initialEmail = formik.getFieldMeta('email').initialValue;
    const newEmail = formik.getFieldMeta('email').value;
    const emailChanged = newEmail !== initialEmail;

    return (
      <Box data-cy="user-profile">
        <UserProfileAvatar />
        <TextField
          variant="standard"
          name="name"
          label={<T>User settings - Full name</T>}
        />
        <TextField
          variant="standard"
          name="email"
          label={<T>User settings - E-mail</T>}
        />
        {userLoadable?.data?.emailAwaitingVerification && (
          <Box>
            <Typography variant="body1">
              <T
                parameters={{
                  email: userLoadable.data.emailAwaitingVerification!,
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
      </Box>
    );
  };

  return (
    <BaseSettingsView
      windowTitle={t('user_profile_title')}
      title={t('user_profile_title')}
      loading={userLoadable.isFetching}
    >
      {userLoadable.data && (
        <StandardForm
          saveActionLoadable={updateUser}
          initialValues={
            {
              password: '',
              passwordRepeat: '',
              name: userLoadable.data.name,
              email: userLoadable.data.username,
            } as UserUpdateDTO
          }
          validationSchema={Validation.USER_SETTINGS}
          onCancel={() => history.goBack()}
          onSubmit={handleSubmit}
        >
          <Fields />
        </StandardForm>
      )}
    </BaseSettingsView>
  );
};
