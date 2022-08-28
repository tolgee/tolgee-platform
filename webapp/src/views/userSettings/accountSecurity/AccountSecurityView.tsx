import React, { FunctionComponent } from 'react';
import { Alert } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useUser } from 'tg.globalContext/helpers';
import { BaseUserSettingsView } from '../BaseUserSettingsView';
import { LINKS } from 'tg.constants/links';
import { ChangePassword } from 'tg.views/userSettings/accountSecurity/ChangePassword';

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
      {user && !isManaged && <ChangePassword />}
      {/* todo: user && <MfaSettings /> */}
    </BaseUserSettingsView>
  );
};
