import { useHistory } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';
import { Box, Paper, styled } from '@mui/material';

import { LINKS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import React from 'react';
import { Key02 } from '@untitled-ui/icons-react';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useIsSsoMigrationRequired, useUser } from 'tg.globalContext/helpers';

export const FULL_PAGE_BREAK_POINT = '(max-width: 700px)';

const StyledContainer = styled(Box)`
  display: grid;
  justify-content: center;
  align-content: center;
`;

const StyledContent = styled(Box)`
  display: grid;
  width: min(100vw, 660px);
  gap: 32px;
`;

const StyledPaper = styled(Paper)`
  padding: 60px;
  display: grid;
  gap: 32px;
  background: ${({ theme }) => theme.palette.tokens.background['paper-1']};
  @media ${FULL_PAGE_BREAK_POINT} {
    padding: 10px;
    box-shadow: none;
    background: transparent;
  }
`;

const SsoMigrationView: React.FC = () => {
  const { useSsoAuthLinkByDomain, refetchInitialData } = useGlobalActions();
  const user = useUser();

  const migrationRequired = useIsSsoMigrationRequired();

  const history = useHistory();
  const { t } = useTranslate();

  useWindowTitle(t('sso_migration_title'));

  const authProviderChangeInfo = useApiQuery({
    url: '/v2/auth-provider/change',
    method: 'get',
    fetchOptions: {
      disableAutoErrorHandle: true,
      disableAuthRedirect: true,
      disableErrorNotification: true,
    },
    options: {
      onError(e) {
        if (e.code && e.code != 'resource_not_found') {
          messageService.error(<TranslatedError code={e.code} />);
        }
      },
    },
  });

  const ssoUrl = useSsoAuthLinkByDomain(user?.domain || '');
  const ssoUrlUnavailable = !ssoUrl.isLoading && !ssoUrl.data?.redirectUrl;
  const ssoUrlValue = ssoUrl.data?.redirectUrl || '';

  if (!migrationRequired) {
    refetchInitialData(); // Avoid loop if initial data are invalid
    history.replace(LINKS.ROOT.build());
  }

  if (authProviderChangeInfo.isLoading) {
    return <FullPageLoading />;
  }

  if (authProviderChangeInfo.data) {
    history.replace(LINKS.ACCEPT_AUTH_PROVIDER_CHANGE.build());
  }

  return (
    <DashboardPage hideQuickStart>
      <StyledContainer>
        <StyledContent>
          <StyledPaper>
            <Box display="grid" gap="24px" justifyItems="center">
              <Box textAlign="center" data-cy="sso-migration-info-text">
                <T
                  keyName="sso_migration_description"
                  params={{ domain: user?.domain || '', br: <br /> }}
                />
              </Box>
              <Box
                display="flex"
                gap={3}
                flexWrap="wrap"
                justifyContent="center"
              >
                <LoadingButton
                  loading={ssoUrl.isLoading}
                  href={ssoUrlValue}
                  disabled={ssoUrlUnavailable}
                  size="medium"
                  endIcon={<Key02 />}
                  variant="outlined"
                  style={{ marginBottom: '0.5rem' }}
                  color="inherit"
                  data-cy="account-security-provider-connect"
                  data-cy-provider="sso"
                >
                  <T keyName="connect_sso" />
                </LoadingButton>
              </Box>
            </Box>
          </StyledPaper>
        </StyledContent>
      </StyledContainer>
    </DashboardPage>
  );
};
export default SsoMigrationView;
