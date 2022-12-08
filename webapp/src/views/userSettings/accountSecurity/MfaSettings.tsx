import React, { FunctionComponent } from 'react';
import { Box, Button, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { useUser } from 'tg.globalContext/helpers';
import { LINKS } from 'tg.constants/links';

const StyledEnabled = styled('span')`
  color: ${({ theme }) => theme.palette.success.main};
`;

const StyledDisabled = styled('span')`
  color: ${({ theme }) => theme.palette.error.main};
`;

export const MfaSettings: FunctionComponent = () => {
  const user = useUser();
  if (!user) return null;

  return (
    <Box>
      <Typography variant="h6">
        <T>account-security-mfa</T>
      </Typography>
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        mt={2}
      >
        <Box>
          <Typography>
            {user.mfaEnabled ? (
              <T params={{ b: <StyledEnabled /> }}>
                account-security-mfa-status-enabled
              </T>
            ) : (
              <T params={{ b: <StyledDisabled /> }}>
                account-security-mfa-status-disabled
              </T>
            )}
          </Typography>
        </Box>
        <Box display="flex" gap={2}>
          {user.mfaEnabled ? (
            <>
              <Button
                variant="outlined"
                color="secondary"
                data-cy="mfa-recovery-button"
                component={Link}
                to={LINKS.USER_ACCOUNT_SECURITY_MFA_RECOVERY.build()}
              >
                <T>account-security-mfa-view-recovery</T>
              </Button>
              <Button
                variant="outlined"
                color="error"
                data-cy="mfa-disable-button"
                component={Link}
                to={LINKS.USER_ACCOUNT_SECURITY_MFA_DISABLE.build()}
              >
                <T>account-security-mfa-disable-mfa-button</T>
              </Button>
            </>
          ) : (
            <Button
              variant="contained"
              color="primary"
              data-cy="mfa-enable-button"
              component={Link}
              to={LINKS.USER_ACCOUNT_SECURITY_MFA_ENABLE.build()}
            >
              <T>account-security-mfa-enable-mfa-button</T>
            </Button>
          )}
        </Box>
      </Box>
    </Box>
  );
};
