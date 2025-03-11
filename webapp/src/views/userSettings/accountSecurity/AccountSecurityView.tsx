import { Fragment, FunctionComponent } from 'react';
import { Redirect, Route } from 'react-router-dom';
import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { LINKS } from 'tg.constants/links';
import { useConfig, useUser } from 'tg.globalContext/helpers';
import { Alert } from 'tg.component/common/Alert';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';

import { BaseUserSettingsView } from '../BaseUserSettingsView';
import { ChangePassword } from './ChangePassword';
import { MfaSettings } from './MfaSettings';
import { EnableMfaDialog } from './EnableMfaDialog';
import { MfaRecoveryCodesDialog } from './MfaRecoveryCodesDialog';
import { DisableMfaDialog } from './DisableMfaDialog';
import { ChangeAuthProvider } from './ChangeAuthProvider';

export const AccountSecurityView: FunctionComponent = () => {
  const { t } = useTranslate();
  const user = useUser();
  const config = useConfig();
  const resetResetLoadable = useApiMutation({
    url: '/api/public/reset_password_request',
    method: 'post',
  });
  const managedBy = useApiQuery({
    url: `/v2/user/managed-by`,
    method: 'get',
  });

  const isManaged = user?.accountType === 'MANAGED';
  const isThirdParty = user?.accountType === 'THIRD_PARTY';

  if (!config.authentication) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  if (isThirdParty) {
    return (
      <BaseUserSettingsView
        windowTitle={t('user-account-security-title')}
        title={t('user-account-security-title')}
        navigation={[
          [
            t('user-account-security-title'),
            LINKS.USER_ACCOUNT_SECURITY.build(),
          ],
        ]}
      >
        <Typography mb={2}>
          <T keyName="account-security-set-password-third-party-info" />
        </Typography>
        <Box>
          {!!resetResetLoadable.error && (
            <Alert severity="error">{resetResetLoadable.error}</Alert>
          )}
          {resetResetLoadable.isSuccess && (
            <Alert
              severity="success"
              data-cy="account-security-set-password-instructions-sent"
            >
              <T keyName="account-security-set-password-instructions-sent" />
            </Alert>
          )}
          <LoadingButton
            data-cy="account-security-initial-password-set"
            color="primary"
            type="submit"
            variant="contained"
            loading={resetResetLoadable.isLoading}
            onClick={() =>
              resetResetLoadable.mutate({
                content: {
                  'application/json': {
                    email: user!.username,
                    callbackUrl: LINKS.RESET_PASSWORD.buildWithOrigin(),
                  },
                },
              })
            }
          >
            <T keyName="account-security-set-password" />
          </LoadingButton>
        </Box>
      </BaseUserSettingsView>
    );
  }

  return (
    <BaseUserSettingsView
      windowTitle={t('user-account-security-title')}
      title={t('user-account-security-title')}
      navigation={[
        [t('user-account-security-title'), LINKS.USER_ACCOUNT_SECURITY.build()],
      ]}
    >
      {isManaged && (
        <Alert severity="info" sx={{ mb: 4 }}>
          {managedBy.isLoading || !managedBy.data ? (
            <T keyName="managed-account-notice" />
          ) : (
            <T
              keyName="managed-account-notice-organization"
              params={{ organization: managedBy.data?.name }}
            />
          )}
        </Alert>
      )}
      {!isManaged && (
        <>
          <ChangePassword />
          <MfaSettings />
          <ChangeAuthProvider />
        </>
      )}

      <Route exact path={LINKS.USER_ACCOUNT_SECURITY_MFA_ENABLE.template}>
        <EnableMfaDialog />
      </Route>
      <Route exact path={LINKS.USER_ACCOUNT_SECURITY_MFA_RECOVERY.template}>
        <MfaRecoveryCodesDialog />
      </Route>
      <Route exact path={LINKS.USER_ACCOUNT_SECURITY_MFA_DISABLE.template}>
        <DisableMfaDialog />
      </Route>
    </BaseUserSettingsView>
  );
};
