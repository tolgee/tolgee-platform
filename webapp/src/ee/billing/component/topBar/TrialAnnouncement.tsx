import React from 'react';
import { TopBarAnnouncementWithIcon } from 'tg.component/layout/TopBar/announcements/TopBarAnnouncementWithIcon';
import { ClockStopwatch } from '@untitled-ui/icons-react';
import { Link } from '@mui/material';
import { T } from '@tolgee/react';
import { Link as RouterLink } from 'react-router-dom';
import { useTrialInfo } from 'tg.component/layout/TopBar/announcements/useTrialInfo';

export const TrialAnnouncement: React.FC = () => {
  const { subscriptionsLink, shouldShowAnnouncement, daysLeft } =
    useTrialInfo();

  if (!shouldShowAnnouncement) {
    return null;
  }

  return (
    <TopBarAnnouncementWithIcon
      icon={<ClockStopwatch />}
      data-cy="topbar-trial-announcement"
    >
      <span>
        <T
          keyName="trial-end-topbar-announcement-message-urgent"
          params={{
            daysLeft: daysLeft,
            link: <Link component={RouterLink} to={subscriptionsLink} />,
          }}
        />
      </span>
    </TopBarAnnouncementWithIcon>
  );
};
