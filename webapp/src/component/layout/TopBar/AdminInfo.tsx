import { Box, Button, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { globalActions } from 'tg.store/global/GlobalActions';

const StyledExitDebugButton = styled(Button)`
  color: inherit;
  border-color: rgba(135, 135, 135, 0.38);
`;

export const AdminInfo = (props: {
  adminAccess: boolean | undefined;
  debuggingCustomerAccount: boolean | undefined;
}) => {
  const history = useHistory();

  return (
    <Box
      flexGrow={1}
      display="flex"
      alignItems="center"
      justifyContent="center"
    >
      {props.adminAccess && (
        <Box data-cy="administration-access-message">
          <T keyName="administration-access-message" />
        </Box>
      )}
      {props.debuggingCustomerAccount && (
        <Box
          display="flex"
          flexGrow={1}
          alignItems="center"
          justifyContent="center"
          data-cy="administration-debug-customer-account-message"
        >
          <Box>
            <T keyName="administration-debugging-customer-account-message" />
          </Box>
          <Box ml={2}>
            <StyledExitDebugButton
              data-cy="administration-debug-customer-exit-button"
              size="small"
              variant="outlined"
              onClick={() => {
                globalActions.exitDebugCustomerAccount.dispatch();
                history.push(LINKS.ADMINISTRATION_USERS.build());
              }}
            >
              <T keyName="administration-exit-debug-customer-account" />
            </StyledExitDebugButton>
          </Box>
        </Box>
      )}
    </Box>
  );
};
