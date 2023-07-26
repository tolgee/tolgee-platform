import React, { FunctionComponent, useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Alert, useMediaQuery } from '@mui/material';
import { useSelector } from 'react-redux';
import { Redirect, useHistory } from 'react-router-dom';
import { container } from 'tsyringe';
import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { SecurityService } from 'tg.service/SecurityService';
import { AppState } from 'tg.store/index';

import { LoginCredentialsForm } from './LoginCredentialsForm';
import { LoginTotpForm } from './LoginTotpForm';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { SPLIT_CONTENT_BREAK_POINT, SplitContent } from '../SplitContent';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { CompactView } from 'tg.component/layout/CompactView';
import { LoginMoreInfo } from './LoginMoreInfo';
import { useReportOnce } from 'tg.hooks/useReportEvent';

interface LoginProps {}

const securityServiceIns = container.resolve(SecurityService);

// noinspection JSUnusedLocalSymbols
export const LoginView: FunctionComponent<LoginProps> = (props) => {
  const { t } = useTranslate();
  const credentialsRef = useRef({ username: '', password: '' });
  const [mfaRequired, setMfaRequired] = useState(false);

  const security = useSelector((state: AppState) => state.global.security);
  const remoteConfig = useConfig();
  const history = useHistory();

  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

  useReportOnce('LOGIN_PAGE_OPENED');

  const authLoading = useSelector(
    (state: AppState) => state.global.authLoading
  );

  if (history.location.state && (history.location.state as any).from) {
    securityServiceIns.saveAfterLoginLink((history.location.state as any).from);
  }

  if (!remoteConfig.authentication || security.allowPrivate) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  if (mfaRequired) {
    return (
      <LoginTotpForm
        credentialsRef={credentialsRef}
        onMfaCancel={() => {
          credentialsRef.current!.password = '';
          setMfaRequired(false);
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
          security.loginErrorCode &&
          !authLoading && (
            <Alert severity="error">
              <TranslatedError code={security.loginErrorCode} />
            </Alert>
          )
        }
        content={
          <SplitContent
            left={
              <LoginCredentialsForm
                credentialsRef={credentialsRef}
                onMfaEnabled={() => setMfaRequired(true)}
              />
            }
            right={<LoginMoreInfo />}
          />
        }
      />
    </DashboardPage>
  );
};
