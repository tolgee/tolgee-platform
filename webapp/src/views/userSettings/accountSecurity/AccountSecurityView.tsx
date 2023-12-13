import React, { FunctionComponent } from 'react';
import { useSelector } from 'react-redux';
import { Redirect, Route } from 'react-router-dom';
import { container } from 'tsyringe';

import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import { useConfig, useUser } from 'tg.globalContext/helpers';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';
import { Alert } from 'tg.component/common/Alert';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { BaseUserSettingsView } from '../BaseUserSettingsView';

import { ChangePassword } from './ChangePassword';
import { MfaSettings } from './MfaSettings';
import { EnableMfaDialog } from './EnableMfaDialog';
import { MfaRecoveryCodesDialog } from './MfaRecoveryCodesDialog';
import { DisableMfaDialog } from './DisableMfaDialog';

const globalActions = container.resolve(GlobalActions);

export const AccountSecurityView: FunctionComponent = () => {
  const { t } = useTranslate();
  const user = useUser();
  const config = useConfig();
  const loadable = useSelector(
    (state: AppState) => state.global.loadables.resetPasswordRequest
  );

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
          {!!loadable.error && <Alert severity="error">{loadable.error}</Alert>}
          {loadable.loaded && (
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
            loading={loadable.loading}
            onClick={() =>
              globalActions.loadableActions.resetPasswordRequest.dispatch(
                user!.username
              )
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
          <T keyName="managed-account-notice" />
        </Alert>
      )}
      {!isManaged && <ChangePassword />}
      <MfaSettings />

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
