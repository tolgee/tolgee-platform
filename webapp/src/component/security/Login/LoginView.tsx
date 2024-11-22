import { FunctionComponent, useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Alert, useMediaQuery, Link as MuiLink } from '@mui/material';
import { Link } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import {
  CompactView,
  SPLIT_CONTENT_BREAK_POINT,
} from 'tg.component/layout/CompactView';
import { useReportOnce } from 'tg.hooks/useReportEvent';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

import { LoginCredentialsForm } from './LoginCredentialsForm';
import { LoginTotpForm } from './LoginTotpForm';
import { LoginMoreInfo } from './LoginMoreInfo';

export const LoginView: FunctionComponent = () => {
  const { t } = useTranslate();
  const credentialsRef = useRef({ username: '', password: '' });
  const [mfaRequired, setMfaRequired] = useState(false);

  const error = useGlobalContext(
    (c) =>
      c.auth.loginLoadable.error ||
      c.auth.authorizeOAuthLoadable.error ||
      c.auth.redirectSsoUrlLoadable.error
  );
  const isLoading = useGlobalContext(
    (c) =>
      c.auth.loginLoadable.isLoading ||
      c.auth.authorizeOAuthLoadable.isLoading ||
      c.auth.redirectSsoUrlLoadable.isLoading
  );

  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

  const registrationAllowed = useGlobalContext(
    (c) =>
      (c.initialData.serverConfiguration.allowRegistrations ||
        c.auth.allowRegistration) &&
      c.initialData.serverConfiguration.nativeEnabled
  );

  useReportOnce('LOGIN_PAGE_OPENED');

  if (mfaRequired) {
    return (
      <LoginTotpForm
        credentialsRef={credentialsRef}
        onMfaCancel={() => {
          setMfaRequired(false);
          credentialsRef.current!.password = '';
        }}
      />
    );
  }

  return (
    <DashboardPage>
      <CompactView
        maxWidth={isSmall ? 550 : 964}
        windowTitle={t('login_title')}
        title={t('login_title')}
        subtitle={
          registrationAllowed ? (
            <T
              keyName="login_subtitle"
              params={{
                link: <MuiLink to={LINKS.SIGN_UP.build()} component={Link} />,
              }}
            />
          ) : undefined
        }
        alerts={
          error?.code &&
          error.code !== 'mfa_enabled' &&
          !isLoading && (
            <Alert severity="error">
              <TranslatedError code={error.code} />
            </Alert>
          )
        }
        primaryContent={
          <LoginCredentialsForm
            credentialsRef={credentialsRef}
            onMfaEnabled={() => setMfaRequired(true)}
          />
        }
        secondaryContent={<LoginMoreInfo />}
      />
    </DashboardPage>
  );
};
