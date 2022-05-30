import { FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';

import { TopBar } from './TopBar/TopBar';

const StyledAppBarSpacer = styled('div')(
  ({ theme }) => theme.mixins.toolbar as any
);

type Props = {
  topBarAutoHide?: boolean;
};

export const DashboardPage: FunctionComponent<React.PropsWithChildren<Props>> =
  ({ children, topBarAutoHide }) => {
    return (
      <Box
        display="flex"
        alignItems="stretch"
        flexDirection="column"
        flexGrow={1}
      >
        <TopBar autoHide={topBarAutoHide} />
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
    );
  };
