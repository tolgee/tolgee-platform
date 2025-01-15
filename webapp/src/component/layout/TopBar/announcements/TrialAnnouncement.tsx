import React from 'react';
import { TopBarAnnouncementWithIcon } from './TopBarAnnouncementWithIcon';
import { ClockStopwatch } from '@untitled-ui/icons-react';
import { Box, Link } from '@mui/material';
import { T } from '@tolgee/react';
import { Link as RouterLink } from 'react-router-dom';
import { useTrialInfo } from './useTrialInfo';

export const TrialAnnouncement: React.FC = () => {
  const { daysLeft, subscriptionsLink, shouldShowAnnouncement } =
    useTrialInfo();

  if (!shouldShowAnnouncement) {
    return null;
  }

  const message =
    daysLeft > 8 ? (
      <T
        keyName="trial-end-topbar-announcement-message"
        params={{ daysLeft: daysLeft }}
      />
    ) : (
      <T
        keyName="trial-end-topbar-announcement-message-urgent"
        params={{
          daysLeft: daysLeft,
          link: <Link component={RouterLink} to={subscriptionsLink} />,
        }}
      />
    );

  return (
    <TopBarAnnouncementWithIcon icon={<ClockStopwatch />}>
      <Box>{message}</Box>
    </TopBarAnnouncementWithIcon>
  );
};
