import { Box, Button, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { FC } from 'react';
import { TopBarAnnouncementWithAlertIcon } from './TopBarAnnouncementWithIcon';

const StyledExitDebugButton = styled(Button)`
  color: inherit;
  border-color: rgba(135, 135, 135, 0.38);
`;

export const DebuggingCustomerAccountAnnouncement: FC = () => {
  const history = useHistory();
  const { exitDebugCustomerAccount } = useGlobalActions();

  const isReadOnlyMode = useGlobalContext(
    (c) => c.initialData.authInfo?.isReadOnly === true
  );

  const message = isReadOnlyMode ? (
    <T keyName="administration-debugging-customer-account-message-read-only" />
  ) : (
    <T keyName="administration-debugging-customer-account-message" />
  );

  return (
    <TopBarAnnouncementWithAlertIcon data-cy="administration-debug-customer-account-message">
      <Box>{message}</Box>
      <Box ml={2}>
        <StyledExitDebugButton
          data-cy="administration-debug-customer-exit-button"
          size="small"
          variant="outlined"
          onClick={() => {
            exitDebugCustomerAccount();
            history.push(LINKS.ADMINISTRATION_USERS.build());
          }}
        >
          <T keyName="administration-exit-debug-customer-account" />
        </StyledExitDebugButton>
      </Box>
    </TopBarAnnouncementWithAlertIcon>
  );
};
