import React, { FunctionComponent } from 'react';
import { Alert, Box, Grid, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';
import { Redirect, useHistory } from 'react-router-dom';
import { container } from 'tsyringe';

import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useConfig, useUser } from 'tg.globalContext/helpers';
import { MessageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { UserUpdateDTO } from 'tg.service/request.types';
import { UserProfileAvatar } from './UserProfileAvatar';
import { BaseUserSettingsView } from '../BaseUserSettingsView';
import { LINKS } from 'tg.constants/links';
import { DeleteUserButton } from './DeleteUserButton';

const messagesService = container.resolve(MessageService);

export const UserProfileView: FunctionComponent = () => {
  const { t } = useTranslate();
  const { refetchInitialData } = useGlobalActions();
  const user = useUser();

  const updateUser = useApiMutation({
    url: '/v2/user',
    method: 'put',
  });

  const handleSubmit = (v: UserUpdateDTO) => {
    if (!v.currentPassword) {
      delete v.currentPassword;
    }

    // @ts-ignore
    v.callbackUrl = window.location.protocol + '//' + window.location.host;
    updateUser.mutate(
      { content: { 'application/json': v } },
      {
        onSuccess() {
          messagesService.success(
            <T keyName="User data - Successfully updated!" />
          );
          refetchInitialData();
        },
      }
    );
  };

  const history = useHistory();
  const config = useConfig();
  const isManaged = user?.accountType === 'MANAGED';

  if (!config.authentication) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  const Fields = () => {
    const formik = useFormikContext();
    const initialEmail = formik.getFieldMeta('email').initialValue;
    const newEmail = formik.getFieldMeta('email').value;
    const emailChanged = newEmail !== initialEmail;

    return (
      <Box data-cy="user-profile" sx={{ mb: 2 }}>
        <Grid container spacing={8}>
          <Grid item xs="auto">
            <UserProfileAvatar />
          </Grid>
          <Grid item xs={12} sm>
            <TextField
              variant="standard"
              name="name"
              label={<T keyName="User settings - Full name" />}
            />
            <TextField
              variant="standard"
              name="email"
              disabled={isManaged}
              helperText={
                isManaged ? t('managed-account-field-hint') : undefined
              }
              label={<T keyName="User settings - E-mail" />}
            />
            {user?.emailAwaitingVerification && (
              <Box>
                <Typography variant="body1">
                  <T
                    keyName="email_waiting_for_verification"
                    params={{
                      email: user.emailAwaitingVerification!,
                    }}
                  />
                </Typography>
              </Box>
            )}

            {emailChanged && config.needsEmailVerification && (
              <Typography variant="body1">
                <T keyName="your_email_was_changed_verification_message" />
              </Typography>
            )}
          </Grid>
        </Grid>

        {emailChanged && (
          <TextField
            name="currentPassword"
            type="password"
            label={<T keyName="current-password" />}
            variant="standard"
          />
        )}
      </Box>
    );
  };

  return (
    <BaseUserSettingsView
      windowTitle={t('user_profile_title')}
      title={t('user_profile_title')}
      navigation={[[t('user_profile_title'), LINKS.USER_PROFILE.build()]]}
    >
      {isManaged && (
        <Alert severity="info" sx={{ mb: 4 }}>
          <T keyName="managed-account-notice" />
        </Alert>
      )}
      {user && (
        <StandardForm
          saveActionLoadable={updateUser}
          customActions={<DeleteUserButton />}
          initialValues={
            {
              name: user.name,
              email: user.username,
              currentPassword: '',
            } as UserUpdateDTO
          }
          validationSchema={Validation.USER_SETTINGS(
            user.accountType,
            user.username
          )}
          onCancel={() => history.goBack()}
          onSubmit={handleSubmit}
        >
          <Fields />
        </StandardForm>
      )}
    </BaseUserSettingsView>
  );
};
