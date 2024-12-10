import { FunctionComponent, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Alert, useMediaQuery } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import {
  CompactView,
  SPLIT_CONTENT_BREAK_POINT,
} from 'tg.component/layout/CompactView';
import {
  useGlobalContext,
  useGlobalActions,
} from 'tg.globalContext/GlobalContext';
import { LoginMoreInfo } from 'tg.component/security/Login/LoginMoreInfo';
import { LoginSsoForm } from 'tg.ee.module/security/Sso/LoginSsoForm';
import { useLocation } from 'react-router-dom';
import { GlobalLoading } from 'tg.component/GlobalLoading';

export const SsoLoginView: FunctionComponent = () => {
  const { t } = useTranslate();
  const { loginRedirectSso, getLastSsoDomain } = useGlobalActions();
  const { search } = useLocation();
  const searchParams = useMemo(() => new URLSearchParams(search), [search]);

  const qAutoSubmit = searchParams.get('submit') === 'true';
  const qDomain = searchParams.get('domain');
  const storedDomain = getLastSsoDomain();
  const defaultDomain = qDomain || storedDomain || '';

  const [isSubmitLoading, setIsSubmitLoading] = useState(qAutoSubmit);

  useEffect(() => {
    if (qAutoSubmit) {
      loginRedirectSso(defaultDomain).catch(() => {
        setIsSubmitLoading(false);
      });
    }
  }, [qAutoSubmit]);

  const credentialsRef = useRef({ domain: defaultDomain });

  const error = useGlobalContext(
    (c) =>
      c.auth.authorizeOAuthLoadable.error || c.auth.redirectSsoUrlLoadable.error
  );
  const isLoading = useGlobalContext(
    (c) =>
      c.auth.authorizeOAuthLoadable.isLoading ||
      c.auth.redirectSsoUrlLoadable.isLoading
  );

  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

  return isSubmitLoading ? (
    <GlobalLoading />
  ) : (
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
