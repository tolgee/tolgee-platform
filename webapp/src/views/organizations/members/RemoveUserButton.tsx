import { IconButton, Tooltip } from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';

import { useOrganization } from '../useOrganization';
import { messageService } from 'tg.service/MessageService';

export const RemoveUserButton = (props: {
  userId: number;
  userName: string;
}) => {
  const { t } = useTranslate();
  const organization = useOrganization();
  const removeUserLoadable = useApiMutation({
    url: '/v2/organizations/{organizationId}/users/{userId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations',
  });

  const removeUser = () => {
    confirmation({
      message: (
        <T
          keyName="really_remove_user_confirmation"
          params={{ userName: props.userName }}
        />
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
              messageService.success(<T keyName="organization_user_deleted" />);
            },
          }
        ),
    });
  };

  return (
    <Tooltip title={t('organization_users_remove_user')}>
      <IconButton
        data-cy="organization-members-remove-user-button"
        onClick={removeUser}
        size="small"
      >
        <XClose />
      </IconButton>
    </Tooltip>
  );
};
