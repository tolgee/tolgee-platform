import React, { FC } from 'react';
import { Button } from '@mui/material';
import { T } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { useUser } from 'tg.globalContext/helpers';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { DeleteUserMessages } from './DeleteUserMessages';

export const DeleteUserButton: FC = () => {
  const { logout } = useGlobalActions();
  const user = useUser();
  const message = useMessage();

  const deleteUser = useApiMutation({
    url: '/v2/user',
    method: 'delete',
    options: {
      onSuccess() {
        message.success(<T keyName="account-deleted-message" />);
        logout();
      },
    },
  });

  if (user?.deletable != true) {
    return null;
  }

  return (
    <Button
      color="error"
      data-cy="delete-user-button"
      variant={'outlined'}
      onClick={() =>
        confirmation({
          onConfirm: () => deleteUser.mutate({}),
          message: <DeleteUserMessages />,
          hardModeText: user!.name,
        })
      }
    >
      <T keyName="delete-user-account-button" />
    </Button>
  );
};
