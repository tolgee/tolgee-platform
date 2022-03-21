import { Button } from '@material-ui/core';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { confirmation } from 'tg.hooks/confirmation';
import { MessageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';

import { useOrganization } from '../useOrganization';

const messageService = container.resolve(MessageService);

export const RemoveUserButton = (props: {
  userId: number;
  userName: string;
}) => {
  const organization = useOrganization();
  const removeUserLoadable = useApiMutation({
    url: '/v2/organizations/{organizationId}/users/{userId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations',
  });

  const removeUser = () => {
    confirmation({
      message: (
        <T parameters={{ userName: props.userName }}>
          really_remove_user_confirmation
        </T>
      ),
      onConfirm: () =>
        removeUserLoadable.mutate(
          {
            path: {
              organizationId: organization!.id,
              userId: props.userId,
            },
          },
          {
            onSuccess: () => {
              messageService.success(<T>organization_user_deleted</T>);
            },
          }
        ),
    });
  };

  return (
    <Button
      data-cy="organization-members-remove-user-button"
      onClick={removeUser}
      variant="outlined"
      size="small"
      aria-controls="simple-menu"
      aria-haspopup="true"
    >
      <T>organization_users_remove_user</T>
    </Button>
  );
};
