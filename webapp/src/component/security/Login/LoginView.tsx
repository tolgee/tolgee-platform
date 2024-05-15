import { FunctionComponent, useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Alert, useMediaQuery } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { CompactView } from 'tg.component/layout/CompactView';
import { useReportOnce } from 'tg.hooks/useReportEvent';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

import { SPLIT_CONTENT_BREAK_POINT } from '../SplitContent';
import { LoginCredentialsForm } from './LoginCredentialsForm';
import { LoginTotpForm } from './LoginTotpForm';
import { LoginMoreInfo } from './LoginMoreInfo';

// noinspection JSUnusedLocalSymbols
export const LoginView: FunctionComponent = () => {
  const { t } = useTranslate();
  const credentialsRef = useRef({ username: '', password: '' });
  const [mfaRequired, setMfaRequired] = useState(false);

  const error = useGlobalContext((c) => c.auth.loginLoadable.error);
  const isLoading = useGlobalContext((c) => c.auth.loginLoadable.isLoading);

  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

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
        maxWidth={isSmall ? 430 : 964}
        windowTitle={t('login_title')}
        title={t('login_title')}
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
