import { PlayCircle } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import { UserAvailabilityButton } from 'tg.views/organizations/members/UserAvailabilityButton';

export const EnableUserButton = (props: {
  userId: number;
  userName: string;
}) => {
  const { t } = useTranslate();
  return (
    <UserAvailabilityButton
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
