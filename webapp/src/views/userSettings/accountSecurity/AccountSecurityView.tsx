import React, { FunctionComponent } from 'react';
import { Route } from 'react-router-dom';
import { Alert } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useUser } from 'tg.globalContext/helpers';
import { BaseUserSettingsView } from '../BaseUserSettingsView';
import { LINKS } from 'tg.constants/links';

import { ChangePassword } from './ChangePassword';
import { MfaSettings } from './MfaSettings';
import { EnableMfaDialog } from './EnableMfaDialog';
import { MfaRecoveryCodesDialog } from './MfaRecoveryCodesDialog';
import { DisableMfaDialog } from './DisableMfaDialog';

export const AccountSecurityView: FunctionComponent = () => {
  const t = useTranslate();
  const user = useUser();
  const isManaged = user?.accountType === 'LDAP';

  return (
    <BaseUserSettingsView
      windowTitle={t('user-account-security-title')}
      title={t('user-account-security-title')}
      navigation={[
        [t('user-account-security-title'), LINKS.USER_ACCOUNT_SECURITY.build()],
      ]}
      containerMaxWidth="md"
    >
      {isManaged && (
        <Alert severity="info" sx={{ mb: 4 }}>
          <T>managed-account-notice</T>
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
