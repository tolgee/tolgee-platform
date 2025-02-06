import { T, useTranslate } from '@tolgee/react';
import { Announcement } from './Announcement';
import { LogIn01 } from '@untitled-ui/icons-react';
import { Box, styled } from '@mui/material';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

const StyledDismiss = styled('div')`
  color: ${({ theme }) => theme.palette.topBanner.linkText};
  text-decoration: underline;
  cursor: pointer;
`;

export const PendingAuthProviderChangeBanner = () => {
  const { t } = useTranslate();
  const { setAuthProviderChange } = useGlobalActions();

  function handleDecline() {
    setAuthProviderChange(false);
  }

  return (
    <Announcement
      content={
        <Box
          sx={{ fontWeight: 'normal' }}
          data-cy="pending-auth-provider-change-banner"
        >
          <T
            keyName="pending_auth_provider_change_description"
            params={{
              b: <b />,
            }}
          />
        </Box>
      }
      icon={<LogIn01 />}
      action={
        <StyledDismiss
          role="button"
          tabIndex={0}
          sx={{ marginLeft: 1 }}
          onClick={handleDecline}
          data-cy="pending-auth-provider-change-dismiss"
        >
          {t('pending_auth_provider_change_decline')}
        </StyledDismiss>
      }
    />
  );
};
