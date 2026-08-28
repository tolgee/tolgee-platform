import { IconButton, Tooltip } from '@mui/material';
import { PlayCircle } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';

import { useOrganization } from '../useOrganization';
import { messageService } from 'tg.service/MessageService';

export const EnableUserButton = (props: {
  userId: number;
  userName: string;
}) => {
  const { t } = useTranslate();
  const organization = useOrganization();
  const enableUserLoadable = useApiMutation({
    url: '/v2/organizations/{organizationId}/users/{userId}/enable',
    method: 'put',
    invalidatePrefix: '/v2/organizations',
  });

  const enableUser = () => {
    confirmation({
      message: (
        <T
          keyName="really_enable_user_confirmation"
          defaultValue="Do you really want to re-enable user {userName}? They will regain access to the organization."
          params={{ userName: props.userName }}
        />
      ),
      onConfirm: () =>
        enableUserLoadable.mutate(
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
                  keyName="organization_user_enabled_message"
                  defaultValue="User re-enabled"
                />
              );
            },
          }
        ),
    });
  };

  return (
    <Tooltip title={t('organization_users_enable_user', 'Re-enable')}>
      <IconButton
        data-cy="organization-members-enable-user-button"
        onClick={enableUser}
        size="small"
      >
        <PlayCircle />
      </IconButton>
    </Tooltip>
  );
};
