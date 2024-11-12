import { FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';

import { TopBar } from './TopBar/TopBar';
import { TopBanner } from './TopBanner/TopBanner';
import { TopSpacer } from './TopSpacer';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { RightSidePanel } from './RightSidePanel';
import { QuickStartGuide } from './QuickStartGuide/QuickStartGuide';
import { useIsEmailVerified } from 'tg.globalContext/helpers';

const StyledMain = styled(Box)`
  display: grid;
  width: 100%;
  min-height: 100%;
  container: main-container / inline-size;
  position: relative;
`;

const StyledHorizontal = styled(Box)`
  display: flex;
  position: relative;
  flex-grow: 1;
  justify-content: stretch;
`;

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

type Props = {
  isAdminAccess?: boolean;
  fixedContent?: React.ReactNode;
};

export const DashboardPage: FunctionComponent<Props> = ({
  children,
  isAdminAccess = false,
  fixedContent,
}) => {
  const isDebuggingCustomerAccount = useGlobalContext(
    (c) => Boolean(c.auth.jwtToken) && Boolean(c.auth.adminToken)
  );

  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);

  const isEmailVerified = useIsEmailVerified();

  const { setQuickStartOpen } = useGlobalActions();
  const quickStartEnabled = useGlobalContext(
    (c) => c.quickStartGuide.enabled && c.initialData.userInfo
  );
  const quickStartOpen = useGlobalContext((c) =>
    Boolean(c.quickStartGuide.open)
  );
  const quickStartFloating = useGlobalContext(
    (c) => c.quickStartGuide.floating
  );

  return (
    <>
      <TopBanner />
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
          isAdminAccess={isAdminAccess}
          isDebuggingCustomerAccount={isDebuggingCustomerAccount}
        />
        <TopSpacer />
        <StyledHorizontal>
          {fixedContent}
          <StyledMain
            component="main"
            sx={{ marginRight: rightPanelWidth + 'px' }}
          >
            {children}
          </StyledMain>
        </StyledHorizontal>
        {quickStartEnabled &&
          (quickStartOpen || quickStartFloating) &&
          isEmailVerified && (
            <RightSidePanel
              open={quickStartOpen}
              onClose={() => setQuickStartOpen(false)}
              floating={quickStartFloating}
            >
              <QuickStartGuide />
            </RightSidePanel>
          )}
      </Box>
    </>
  );
};
