import { Button } from '@material-ui/core';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';
import { useOrganization } from '../useOrganization';
import { confirmation } from '../../../../hooks/confirmation';
import { useDeleteOrganizationUser } from '../../../../service/hooks/Organization';
import { useQueryClient } from 'react-query';
import { MessageService } from '../../../../service/MessageService';

const messageService = container.resolve(MessageService);

const OrganizationRemoveUserButton = (props: {
  userId: number;
  userName: string;
}) => {
  const queryClient = useQueryClient();
  const organization = useOrganization();
  const removeUserLoadable = useDeleteOrganizationUser(organization!.id);

  const removeUser = () => {
    confirmation({
      message: (
        <T parameters={{ userName: props.userName }}>
          really_remove_user_confirmation
        </T>
      ),
      onConfirm: () =>
        removeUserLoadable.mutate(props.userId, {
          onSuccess: () => {
            messageService.success(<T>organization_user_deleted</T>);
            queryClient.invalidateQueries(['organizations'], {
              refetchActive: true,
            });
          },
        }),
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

export default OrganizationRemoveUserButton;
