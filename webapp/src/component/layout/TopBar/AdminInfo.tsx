import { Box, Button, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';

const StyledExitDebugButton = styled(Button)`
  color: inherit;
  border-color: rgba(255, 255, 255, 0.38);
`;

const globalActions = container.resolve(GlobalActions);

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
          <T>administration-access-message</T>
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
            <T>administration-debugging-customer-account-message</T>
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
              <T>administration-exit-debug-customer-account</T>
            </StyledExitDebugButton>
          </Box>
        </Box>
      )}
    </Box>
  );
};
