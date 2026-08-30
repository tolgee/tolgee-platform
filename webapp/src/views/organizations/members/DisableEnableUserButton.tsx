import { IconButton, Tooltip } from '@mui/material';
import { PauseCircle, PlayCircle } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { messageService } from 'tg.service/MessageService';

type Props = {
  userId: number;
  dataCy: string;
  icon: React.ReactNode;
  tooltip: string;
  confirmMessage: React.ReactNode;
  successMessage: React.ReactNode;
  url:
    | '/v2/organizations/{organizationId}/users/{userId}/disable'
    | '/v2/organizations/{organizationId}/users/{userId}/enable';
};

const ToggleUserEnabledButton = (props: Props) => {
  const organization = useOrganization();
  const toggleLoadable = useApiMutation({
    url: props.url,
    method: 'put',
    invalidatePrefix: '/v2/organizations',
  });

  const toggleUser = () => {
    confirmation({
      message: props.confirmMessage,
      onConfirm: () =>
        toggleLoadable.mutate(
          {
            path: {
              organizationId: organization!.id,
              userId: props.userId,
            },
          },
          {
            onSuccess: () => {
              messageService.success(props.successMessage);
            },
          }
        ),
    });
  };

  return (
    <Tooltip title={props.tooltip}>
      <IconButton data-cy={props.dataCy} onClick={toggleUser} size="small">
        {props.icon}
      </IconButton>
    </Tooltip>
  );
};

export const DisableUserButton = (props: {
  userId: number;
  userName: string;
}) => {
  const { t } = useTranslate();
  return (
    <ToggleUserEnabledButton
      userId={props.userId}
      dataCy="organization-members-disable-user-button"
      url="/v2/organizations/{organizationId}/users/{userId}/disable"
      icon={<PauseCircle />}
      tooltip={t('organization_users_disable_user', 'Disable')}
      confirmMessage={
        <T
          keyName="really_disable_user_confirmation"
          defaultValue="Do you really want to disable user {userName}? They will lose access to the organization until you re-enable them."
          params={{ userName: props.userName }}
        />
      }
      successMessage={
        <T
          keyName="organization_user_disabled_message"
          defaultValue="User disabled"
        />
      }
    />
  );
};

export const EnableUserButton = (props: {
  userId: number;
  userName: string;
}) => {
  const { t } = useTranslate();
  return (
    <ToggleUserEnabledButton
      userId={props.userId}
      dataCy="organization-members-enable-user-button"
      url="/v2/organizations/{organizationId}/users/{userId}/enable"
      icon={<PlayCircle />}
      tooltip={t('organization_users_enable_user', 'Re-enable')}
      confirmMessage={
        <T
          keyName="really_enable_user_confirmation"
          defaultValue="Do you really want to re-enable user {userName}? They will regain access to the organization."
          params={{ userName: props.userName }}
        />
      }
      successMessage={
        <T
          keyName="organization_user_enabled_message"
          defaultValue="User re-enabled"
        />
      }
    />
  );
};
