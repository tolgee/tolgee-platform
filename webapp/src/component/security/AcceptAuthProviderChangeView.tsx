import { useHistory } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';

import { LINKS } from 'tg.constants/links';
import { messageService } from 'tg.service/MessageService';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import React, { Fragment, useEffect, useState } from 'react';
import { useIsSsoMigrationRequired } from 'tg.globalContext/helpers';
import { AuthProviderChangeBody } from 'tg.component/security/AuthProviderChangeBody';

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

const AcceptAuthProviderChangeView: React.FC = () => {
  const history = useHistory();
  const { t } = useTranslate();

  useWindowTitle(t('auth_provider_migration_title'));

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
      {
        content: {
          'application/json': {
            id: authProviderChangeInfo.data?.id || '',
          },
        },
      },
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

  const authType = authProviderChangeInfo.data?.authType;
  const willBeManaged = authType === 'SSO' || authType === 'SSO_GLOBAL';

  const [autoAccepted, setAutoAccepted] = useState(false);
  useEffect(() => {
    // Auto-accept forced sso migration to avoid second confirmation dialog
    if (
      authProviderChangeInfo.data &&
      willBeManaged &&
      isSsoMigrationRequired &&
      !autoAccepted
    ) {
      setAutoAccepted(true);
      handleAccept();
    }
  }, [
    authProviderChangeInfo,
    willBeManaged,
    isSsoMigrationRequired,
    autoAccepted,
  ]);

  if (!authProviderChangeInfo.data || authProviderCurrentInfo.isLoading) {
    return <FullPageLoading />;
  }

  const buttons = (
    <>
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
    </>
  );

  return (
    <DashboardPage hideQuickStart>
      <StyledContainer>
        <StyledContent>
          <AuthProviderChangeBody
            willBeManaged={willBeManaged}
            authType={authType ?? 'NONE'}
            authTypeOld={authProviderCurrentInfo.data?.authType ?? 'NONE'}
            ssoDomain={authProviderChangeInfo.data?.ssoDomain ?? ''}
          >
            {buttons}
          </AuthProviderChangeBody>
        </StyledContent>
      </StyledContainer>
    </DashboardPage>
  );
};
export default AcceptAuthProviderChangeView;
