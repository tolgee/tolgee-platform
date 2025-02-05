import React, { ComponentProps, FC, ReactNode } from 'react';
import { Box } from '@mui/material';
import { AlertTriangle } from '@untitled-ui/icons-react';

export const TopBarAnnouncementWithIcon: FC<
  {
    icon: ReactNode;
  } & ComponentProps<typeof Box>
> = ({ icon, children, ...props }) => {
  return (
    <Box
      sx={{
        display: 'flex',
        fontSize: '15px',
        textAlign: 'left',
        mx: 4,
        alignItems: 'center',
      }}
      {...props}
    >
      <Box mr={'12px'} display="flex">
        {icon}
      </Box>
      {children}
    </Box>
  );
};

export const TopBarAnnouncementWithAlertIcon: FC = ({ children, ...props }) => {
  return (
    <TopBarAnnouncementWithIcon icon={<AlertTriangle />} {...props}>
      {children}
    </TopBarAnnouncementWithIcon>
  );
};
