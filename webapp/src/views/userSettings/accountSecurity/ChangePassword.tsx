import React, { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';
import { container } from 'tsyringe';

import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useUser } from 'tg.globalContext/helpers';
import { MessageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { UserUpdatePasswordDTO } from 'tg.service/request.types';
import { SetPasswordFields } from 'tg.component/security/SetPasswordFields';

const messagesService = container.resolve(MessageService);

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
        onSuccess() {
          messagesService.success(<T>password-updated</T>);
        },
      }
    );
  };

  const history = useHistory();

  // todo: third party initial password set flow
  // todo: consider installations where there are no SMTP server configured?

  return (
    <Box>
      <Typography variant="h6">
        <T>Password</T>
      </Typography>
      {user && (
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
          onCancel={() => history.goBack()}
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
      )}
    </Box>
  );
};
