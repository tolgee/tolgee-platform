import { FC } from 'react';
import { T } from '@tolgee/react';
import { TopBarAnnouncementWithAlertIcon } from './TopBarAnnouncementWithIcon';

export const AdministrationAccessAnnouncement: FC = () => {
  return (
    <TopBarAnnouncementWithAlertIcon data-cy="administration-access-message">
      <T keyName="administration-access-message" />
    </TopBarAnnouncementWithAlertIcon>
  );
};
