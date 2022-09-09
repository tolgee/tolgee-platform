import React, { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { MessageService } from 'tg.service/MessageService';
import { SecurityService } from 'tg.service/SecurityService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { UserUpdatePasswordDTO } from 'tg.service/request.types';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { SetPasswordFields } from 'tg.component/security/SetPasswordFields';
import { useUser } from 'tg.globalContext/helpers';
import { Validation } from 'tg.constants/GlobalValidationSchema';

const messagesService = container.resolve(MessageService);
const securityService = container.resolve(SecurityService);

export const ChangePassword: FunctionComponent = () => {
  const user = useUser();

  const updatePassword = useApiMutation({
    url: '/v2/user/password',
    method: 'put',
  });

  const handleSubmit = (v: UserUpdatePasswordDTO) => {
    updatePassword.mutate(
      { content: { 'application/json': v } },
      {
        onSuccess(r) {
          securityService.setToken(r.accessToken!);
          messagesService.success(<T>password-updated</T>);
        },
      }
    );
  };

  if (!user) return null;

  return (
    <Box>
      <Typography variant="h6">
        <T>Password</T>
      </Typography>
      <StandardForm
        saveActionLoadable={updatePassword}
        initialValues={
          {
            currentPassword: '',
            password: '',
            passwordRepeat: '',
          } as UserUpdatePasswordDTO
        }
        validationSchema={Validation.USER_PASSWORD_CHANGE}
        onSubmit={handleSubmit}
      >
        <TextField
          name="currentPassword"
          type="password"
          label={<T>current-password</T>}
          variant="standard"
        />
        <SetPasswordFields />
      </StandardForm>
    </Box>
  );
};
