import { IconButton, Tooltip } from '@mui/material';
import { PauseCircle } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';

import { useOrganization } from '../useOrganization';
import { messageService } from 'tg.service/MessageService';

export const DisableUserButton = (props: {
  userId: number;
  userName: string;
}) => {
  const { t } = useTranslate();
  const organization = useOrganization();
  const disableUserLoadable = useApiMutation({
    url: '/v2/organizations/{organizationId}/users/{userId}/disable',
    method: 'put',
    invalidatePrefix: '/v2/organizations',
  });

  const disableUser = () => {
    confirmation({
      message: (
        <T
          keyName="really_disable_user_confirmation"
          defaultValue="Do you really want to disable user {userName}? They will lose access to the organization until you re-enable them."
          params={{ userName: props.userName }}
        />
      ),
      onConfirm: () =>
        disableUserLoadable.mutate(
          {
            path: {
              organizationId: organization!.id,
              userId: props.userId,
            },
          },
          {
            onSuccess: () => {
              messageService.success(
                <T
                  keyName="organization_user_disabled_message"
                  defaultValue="User disabled"
                />
              );
            },
          }
        ),
    });
  };

  return (
    <Tooltip title={t('organization_users_disable_user', 'Disable')}>
      <IconButton
        data-cy="organization-members-disable-user-button"
        onClick={disableUser}
        size="small"
      >
        <PauseCircle />
      </IconButton>
    </Tooltip>
  );
};
