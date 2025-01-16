import React, { FC, ReactNode } from 'react';
import { Box } from '@mui/material';
import { AlertTriangle } from '@untitled-ui/icons-react';

export const TopBarAnnouncementWithIcon: FC<{
  icon: ReactNode;
}> = ({ icon, children }) => {
  return (
    <Box
      sx={{
        display: 'flex',
        fontSize: '15px',
        textAlign: 'left',
        mx: 4,
        alignItems: 'center',
      }}
    >
      <Box mr={'12px'} display="flex">
        {icon}
      </Box>
      {children}
    </Box>
  );
};

export const TopBarAnnouncementWithAlertIcon: FC = ({ children }) => {
  return (
    <TopBarAnnouncementWithIcon icon={<AlertTriangle />}>
      {children}
    </TopBarAnnouncementWithIcon>
  );
};
