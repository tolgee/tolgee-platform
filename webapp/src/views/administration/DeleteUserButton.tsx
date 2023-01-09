import { useApiMutation } from 'tg.service/http/useQueryApi';
import { Button } from '@mui/material';
import { T } from '@tolgee/react';
import { useUser } from 'tg.globalContext/helpers';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { components } from 'tg.service/apiSchema.generated';

export const DeleteUserButton = (props: {
  user: components['schemas']['UserAccountModel'];
}) => {
  const loggedInUser = useUser();
  const message = useMessage();

  const deleteUser = useApiMutation({
    url: '/v2/administration/users/{userId}',
    method: 'delete',
    invalidatePrefix: '/v2/administration',
    options: {
      onSuccess() {
        message.success(<T>administration_user_deleted_message</T>);
      },
    },
  });

  return (
    <Button
      sx={{ mr: 1 }}
      disabled={loggedInUser?.id === props.user.id}
      data-cy="administration-user-delete-user"
      size="small"
      variant="outlined"
      color="error"
      onClick={() => {
        confirmation({
          message: (
            <T params={{ name: props.user.name || props.user.username }}>
              delete-user-confirmation-message
            </T>
          ),
          onConfirm() {
            deleteUser.mutate({ path: { userId: props.user.id } });
          },
        });
      }}
    >
      <T>administration_delete_user_button</T>
    </Button>
  );
};
