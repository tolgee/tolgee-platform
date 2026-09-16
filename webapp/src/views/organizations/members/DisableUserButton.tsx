import { PauseCircle } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import { UserAvailabilityButton } from 'tg.views/organizations/members/UserAvailabilityButton';

export const DisableUserButton = (props: {
  userId: number;
  userName: string;
}) => {
  const { t } = useTranslate();
  return (
    <UserAvailabilityButton
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
