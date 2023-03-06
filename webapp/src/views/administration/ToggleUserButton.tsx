import { useApiMutation } from 'tg.service/http/useQueryApi';
import { Button } from '@mui/material';
import { T } from '@tolgee/react';
import { useUser } from 'tg.globalContext/helpers';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { components } from 'tg.service/apiSchema.generated';

export const ToggleUserButton = (props: {
  user: components['schemas']['UserAccountModel'];
}) => {
  const loggedInUser = useUser();
  const message = useMessage();

  const disableUser = useApiMutation({
    url: '/v2/administration/users/{userId}/disable',
    method: 'put',
    invalidatePrefix: '/v2/administration',
    options: {
      onSuccess() {
        message.success(<T>administration_user_disabled_message</T>);
      },
    },
  });

  const enableUser = useApiMutation({
    url: '/v2/administration/users/{userId}/enable',
    method: 'put',
    invalidatePrefix: '/v2/administration',
    options: {
      onSuccess() {
        message.success(<T>administration_user_enabled_message</T>);
      },
    },
  });

  if (props.user.disabled) {
    return (
      <Button
        sx={{ mr: 1 }}
        disabled={loggedInUser?.id === props.user.id}
        data-cy="administration-user-enable-user"
        size="small"
        variant="outlined"
        color="error"
        onClick={() => {
          enableUser.mutate({ path: { userId: props.user.id } });
        }}
      >
        <T>administration_enable_user_button</T>
      </Button>
    );
  }

  return (
    <Button
      sx={{ mr: 1 }}
      disabled={loggedInUser?.id === props.user.id}
      data-cy="administration-user-disable-user"
      size="small"
      variant="outlined"
      color="error"
      onClick={() => {
        confirmation({
          message: (
            <T params={{ name: props.user.name || props.user.username }}>
              disable-user-confirmation-message
            </T>
          ),
          onConfirm() {
            disableUser.mutate({ path: { userId: props.user.id } });
          },
        });
      }}
    >
      <T>administration_disable_user_button</T>
    </Button>
  );
};
