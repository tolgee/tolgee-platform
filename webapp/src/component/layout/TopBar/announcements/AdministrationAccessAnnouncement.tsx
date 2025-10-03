import { FC } from 'react';
import { T } from '@tolgee/react';
import { TopBarAnnouncementWithAlertIcon } from './TopBarAnnouncementWithIcon';
import { useIsSupporter } from 'tg.globalContext/helpers';

export const AdministrationAccessAnnouncement: FC = () => {
  const isSupporter = useIsSupporter();

  const message = isSupporter ? (
    <T keyName="administration-access-message-supporter" />
  ) : (
    <T keyName="administration-access-message" />
  );

  return (
    <TopBarAnnouncementWithAlertIcon data-cy="administration-access-message">
      {message}
    </TopBarAnnouncementWithAlertIcon>
  );
};
