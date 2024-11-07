import { FunctionComponent, useRef } from 'react';
import { useTranslate } from '@tolgee/react';
import { Alert, useMediaQuery } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import {
  CompactView,
  SPLIT_CONTENT_BREAK_POINT,
} from 'tg.component/layout/CompactView';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { LoginMoreInfo } from 'tg.component/security/Login/LoginMoreInfo';
import { LoginSsoForm } from 'tg.component/security/Sso/LoginSsoForm';

export const SsoLoginView: FunctionComponent = () => {
  const { t } = useTranslate();
  const credentialsRef = useRef({ domain: '' });

  const error = useGlobalContext((c) => c.auth.authorizeOAuthLoadable.error);
  const isLoading = useGlobalContext(
    (c) => c.auth.authorizeOAuthLoadable.isLoading
  );

  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

  return (
    <DashboardPage>
      <CompactView
        maxWidth={isSmall ? 550 : 964}
        windowTitle={t('login_sso_title')}
        title={t('login_sso_title')}
        alerts={
          error?.code &&
          error.code !== 'mfa_enabled' &&
          !isLoading && (
            <Alert severity="error">
              <TranslatedError code={error.code} />
            </Alert>
          )
        }
        primaryContent={<LoginSsoForm credentialsRef={credentialsRef} />}
        secondaryContent={<LoginMoreInfo />}
      />
    </DashboardPage>
  );
};
