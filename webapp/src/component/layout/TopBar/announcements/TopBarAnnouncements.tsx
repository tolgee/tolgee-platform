import React, { FC, useMemo } from 'react';
import { Box } from '@mui/material';
import { AdministrationAccessAnnouncement } from './AdministrationAccessAnnouncement';
import { DebuggingCustomerAccountAnnouncement } from './DebuggingCustomerAccountAnnouncement';
import { TrialAnnouncement } from 'tg.ee';

type TopBarAnnouncementsProps = {
  isAdminAccess?: boolean;
  isDebuggingCustomerAccount?: boolean;
};

export const TopBarAnnouncements: FC<TopBarAnnouncementsProps> = ({
  isAdminAccess,
  isDebuggingCustomerAccount,
}) => {
  function getContents() {
    if (isAdminAccess) {
      return <AdministrationAccessAnnouncement />;
    }

    if (isDebuggingCustomerAccount) {
      return <DebuggingCustomerAccountAnnouncement />;
    }

    return <TrialAnnouncement />;
  }

  const contents = useMemo(
    () => getContents(),
    [isAdminAccess, isDebuggingCustomerAccount]
  );

  return (
    <>
      <Box
        flexGrow={1}
        display="flex"
        alignItems="center"
        justifyContent="center"
        fontSize={15}
        fontWeight={700}
      >
        {contents}
      </Box>
    </>
  );
};
