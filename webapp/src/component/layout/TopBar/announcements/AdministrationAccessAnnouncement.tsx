import { FC } from 'react';
import { T } from '@tolgee/react';
import { TopBarAnnouncementWithAlertIcon } from './TopBarAnnouncementWithIcon';

export const AdministrationAccessAnnouncement: FC = () => {
  return (
    <TopBarAnnouncementWithAlertIcon>
      <T keyName="administration-access-message" />
    </TopBarAnnouncementWithAlertIcon>
  );
};
