import { FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';

import { TopBar } from './TopBar/TopBar';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';

const StyledAppBarSpacer = styled('div')(
  ({ theme }) => theme.mixins.toolbar as any
);

type Props = {
  topBarAutoHide?: boolean;
  isAdminAccess?: boolean;
};

export const DashboardPage: FunctionComponent<Props> = ({
  children,
  topBarAutoHide,
  isAdminAccess = false,
}) => {
  const AdminFrame = styled(Box)`
    border: 5px solid rgba(255, 0, 0, 0.7);
    position: fixed;
    z-index: 1204;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
  `;

  const security = useSelector((state: AppState) => state.global.security);

  const isDebuggingCustomerAccount =
    !!security.adminJwtToken && !!security.jwtToken;

  return (
    <>
      {(isAdminAccess || isDebuggingCustomerAccount) && (
        <AdminFrame data-cy="administration-frame" />
      )}
      <Box
        display="flex"
        alignItems="stretch"
        flexDirection="column"
        flexGrow={1}
      >
        <TopBar
          autoHide={topBarAutoHide}
          isAdminAccess={isAdminAccess}
          isDebuggingCustomerAccount={isDebuggingCustomerAccount}
        />
        <StyledAppBarSpacer />
        <Box
          component="main"
          position="relative"
          display="flex"
          flexGrow="1"
          justifyContent="stretch"
        >
          {children}
        </Box>
      </Box>
    </>
  );
};
