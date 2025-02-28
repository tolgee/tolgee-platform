import { useHistory } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';
import { Box, Paper, styled, Typography } from '@mui/material';

import { LINKS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import React from 'react';
import { useIsSsoMigrationRequired } from 'tg.globalContext/helpers';

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

  const { handleAfterLogin } = useGlobalActions();
  const isSsoMigrationRequired = useIsSsoMigrationRequired();

  const acceptChange = useApiMutation({
    url: '/v2/auth-provider/change',
    method: 'post',
    fetchOptions: {
      disableAutoErrorHandle: true,
    },
  });

  const rejectChange = useApiMutation({
    url: '/v2/auth-provider/change',
    method: 'delete',
    fetchOptions: {
      disableAutoErrorHandle: true,
    },
  });

  const authProviderCurrentInfo = useApiQuery({
    url: '/v2/auth-provider',
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
        history.replace(LINKS.USER_ACCOUNT_SECURITY.build());
        if (e.code) {
          messageService.error(<TranslatedError code={e.code} />);
        }
      },
    },
  });

  function handleAccept() {
    acceptChange.mutate(
      {},
      {
        onSuccess(r) {
          handleAfterLogin(r);
          messageService.success(<T keyName="auth_provider_change_accepted" />);
        },
        onSettled() {
          history.replace(LINKS.USER_ACCOUNT_SECURITY.build());
        },
      }
    );
  }

  function handleReject() {
    rejectChange.mutate(
      {},
      {
        onSuccess(r) {
          messageService.success(<T keyName="auth_provider_change_rejected" />);
        },
        onSettled() {
          history.replace(LINKS.USER_ACCOUNT_SECURITY.build());
        },
      }
    );
  }

  if (!authProviderChangeInfo.data || authProviderCurrentInfo.isLoading) {
    return <FullPageLoading />;
  }

  let titleText: string;
  let infoText: React.ReactNode;

  const accountType = authProviderChangeInfo.data.accountType;
  const authType = authProviderChangeInfo.data.authType;
  const authTypeOld = authProviderCurrentInfo.data?.authType ?? 'NONE';
  const ssoDomain = authProviderChangeInfo.data.ssoDomain;
  const params = {
    authType,
    authTypeOld,
    ssoDomain,
    b: <b />,
  };

  const willBeManaged = accountType === 'MANAGED';

  if (willBeManaged) {
    titleText = t('accept_auth_provider_change_managed_sso_title');
    infoText = (
      <T
        keyName="accept_auth_provider_change_description_managed_sso"
        params={params}
      />
    );
  } else {
    titleText = t('accept_auth_provider_change_title');
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
              {titleText}
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
                  loading={acceptChange.isLoading || rejectChange.isLoading}
                  variant="contained"
                  color="primary"
                  onClick={handleAccept}
                  data-cy="accept-auth-provider-change-accept"
                >
                  {willBeManaged
                    ? t('accept_auth_provider_change_accept')
                    : t('accept_auth_provider_change_accept_non_managed')}
                </LoadingButton>
                {(!willBeManaged || !isSsoMigrationRequired) && (
                  <LoadingButton
                    loading={acceptChange.isLoading || rejectChange.isLoading}
                    variant="outlined"
                    onClick={handleReject}
                    data-cy="accept-auth-provider-change-decline"
                  >
                    {t('accept_auth_provider_change_decline')}
                  </LoadingButton>
                )}
              </Box>
            </Box>
          </StyledPaper>
        </StyledContent>
      </StyledContainer>
    </DashboardPage>
  );
};
export default AcceptAuthProviderChangeView;
