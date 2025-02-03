import { useHistory } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, Link, Paper, styled, Typography } from '@mui/material';

import { LINKS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

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

const AcceptAuthProviderChangeView: React.FC = () => {
  const history = useHistory();
  const { t } = useTranslate();

  useWindowTitle(t('accept_auth_provider_change_title'));

  const { setAuthProviderChange } = useGlobalActions();

  const acceptChange = useApiMutation({
    url: '/api/auth_provider/changed/accept',
    method: 'post',
  });

  const authProviderChangeInfo = useApiQuery({
    url: '/api/auth_provider/changed',
    method: 'get',
    options: {
      onError(e) {
        setAuthProviderChange(false);
        history.replace(LINKS.PROJECT.build());
        if (e.code) {
          messageService.error(<TranslatedError code={e.code} />);
        }
      },
    },
  });

  function handleAccept() {
    acceptChange.mutate({
      onSuccess() {
        setAuthProviderChange(false);
        messageService.success(<T keyName="auth_provider_change_accepted" />);
      },
      onSettled() {
        history.replace(LINKS.PROJECTS.build());
      },
    });
  }

  function handleDecline() {
    setAuthProviderChange(false);
    history.push(LINKS.LOGIN.build());
  }

  if (!authProviderChangeInfo.data) {
    return <FullPageLoading />;
  }

  let infoText: React.ReactNode = null;

  const accountType = authProviderChangeInfo.data.accountType;
  const authType = authProviderChangeInfo.data.authType;
  const ssoDomain = authProviderChangeInfo.data.ssoDomain;
  const params = {
    ssoDomain,
    b: <b />,
  };

  if (accountType === 'MANAGED') {
    if (authType == 'SSO' && ssoDomain) {
      infoText = (
        <T
          keyName="accept_auth_provider_change_description_managed_sso"
          params={params}
        />
      );
    } else {
      infoText = (
        <T
          keyName="accept_auth_provider_change_description_managed"
          params={params}
        />
      );
    }
  } else {
    infoText = (
      <T keyName="accept_auth_provider_change_description" params={params} />
    );
  }

  return (
    <DashboardPage hideQuickStart>
      <StyledContainer>
        <StyledContent>
          <StyledPaper>
            <Typography variant="h3" sx={{ textAlign: 'center' }}>
              {t('accept_auth_provider_change_title')}
            </Typography>

            <Box display="grid" gap="24px" justifyItems="center">
              <Box
                textAlign="center"
                data-cy="accept-auth-provider-change-info-text"
              >
                {infoText}
              </Box>
              <Box
                display="flex"
                gap={3}
                flexWrap="wrap"
                justifyContent="center"
              >
                <LoadingButton
                  loading={acceptChange.isLoading}
                  variant="contained"
                  color="primary"
                  onClick={handleAccept}
                  data-cy="accept-auth-provider-change-accept"
                >
                  {t('accept_auth_provider_change_accept')}
                </LoadingButton>
                <Button
                  variant="outlined"
                  onClick={handleDecline}
                  data-cy="accept-auth-provider-change-decline"
                >
                  {t('accept_auth_provider_change_decline')}
                </Button>
              </Box>
            </Box>
          </StyledPaper>
          <Box display="flex" justifyContent="center">
            <Link href="https://tolgee.io">
              {t('accept_auth_provider_change_learn_more')}
            </Link>
          </Box>
        </StyledContent>
      </StyledContainer>
    </DashboardPage>
  );
};
// TODO: learn more link to docs
export default AcceptAuthProviderChangeView;
