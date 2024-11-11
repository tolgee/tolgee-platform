import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import { Box, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { globalContext } from 'tg.globalContext/globalActions';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import React from 'react';
import { Alert } from 'tg.component/common/Alert';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

const StyledContainer = styled('div')`
  padding: 28px 4px 0px 4px;
  display: grid;
  justify-items: center;
`;

const StyledTitle = styled('h2')`
  color: ${({ theme }) => theme.palette.text.primary}
  font-size: 24px;
  font-style: normal;
  font-weight: 400;
  padding-top: 40px;
  margin-top: 0px;
  text-align: center;
`;

const StyledDescription = styled('div')`
  color: ${({ theme }) => theme.palette.text.primary}
  font-size: 15px;
  padding-bottom: 60px;
  max-width: 550px;
  text-align: center;
`;

const LOCAL_STORAGE_CHANGE_PROVIDER = 'change_provider';

function getProviderName(accountType?: string, authType?: string) {
  if (accountType === 'LOCAL') {
    return 'username and password';
  } else if (accountType === 'THIRD_PARTY') {
    return authType || 'Third-Party Provider';
  }
  return 'Unknown Provider';
}

export function ChangeAuthProviderView() {
  const { t } = useTranslate();
  const requestIdString = localStorage.getItem(LOCAL_STORAGE_CHANGE_PROVIDER);
  const requestId = requestIdString ? Number(requestIdString) : 0;

  const { data } = useApiQuery({
    url: '/api/public/auth-provider/get-request',
    method: 'get',
    query: {
      requestId,
    },
  });

  const { error, isSuccess, isLoading, mutate } = useApiMutation({
    url: '/api/public/auth-provider/request-change',
    method: 'post',
  });

  const currentProvider = getProviderName(
    data?.oldAccountType,
    data?.oldAuthType
  );
  const newProvider = getProviderName(data?.newAccountType, data?.newAuthType);

  const confirmChange = () => {
    mutate({
      content: {
        'application/json': {
          changeRequestId: requestId,
          isConfirmed: true,
        },
      },
    });

    if (isSuccess) {
      globalContext.actions?.redirectTo(LINKS.LOGIN.build());
    }
  };

  const discardChange = () => {
    mutate({
      content: {
        'application/json': {
          changeRequestId: requestId,
          isConfirmed: false,
        },
      },
    });

    if (isSuccess) {
      globalContext.actions?.redirectTo(LINKS.LOGIN.build());
    }
  };

  return (
    <DashboardPage>
      <CompactView
        alerts={
          error?.code &&
          !isLoading && (
            <Alert severity="error">
              <TranslatedError code={error.code} />
            </Alert>
          )
        }
        maxWidth={800}
        windowTitle={t('slack_connect_title')}
        primaryContent={
          <StyledContainer>
            <Box display="flex" justifyContent="center"></Box>
            <StyledTitle>
              <T keyName="change_auth_provider_title" />
            </StyledTitle>

            <StyledDescription>
              <T
                keyName="change_auth_provider_description"
                params={{
                  current_provider: currentProvider,
                  new_provider: newProvider,
                  b: <strong />,
                }}
              />
            </StyledDescription>

            <Box display="flex" gap={2} mb={1.5}>
              <LoadingButton
                size="medium"
                variant="outlined"
                loading={isLoading}
                onClick={discardChange}
              >
                <T keyName="change_auth_provider_cancel" />
              </LoadingButton>
              <LoadingButton
                size="medium"
                variant="contained"
                color="primary"
                loading={isLoading}
                onClick={confirmChange}
              >
                <T keyName="change_auth_provider_confirm" />
              </LoadingButton>
            </Box>
          </StyledContainer>
        }
      />
    </DashboardPage>
  );
}
