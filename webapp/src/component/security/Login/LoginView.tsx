import React, { FunctionComponent, useRef, useState } from 'react';
import { useSelector } from 'react-redux';
import { Redirect, useHistory } from 'react-router-dom';
import { container } from 'tsyringe';
import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { SecurityService } from 'tg.service/SecurityService';
import { AppState } from 'tg.store/index';

import { LoginCredentialsForm } from './LoginCredentialsForm';
import { LoginTotpForm } from './LoginTotpForm';

interface LoginProps {}

const securityServiceIns = container.resolve(SecurityService);

// noinspection JSUnusedLocalSymbols
export const LoginView: FunctionComponent<LoginProps> = (props) => {
  const credentialsRef = useRef({ username: '', password: '' });
  const [mfaRequired, setMfaRequired] = useState(false);

  const security = useSelector((state: AppState) => state.global.security);
  const remoteConfig = useConfig();
  const history = useHistory();

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
    <LoginCredentialsForm
      credentialsRef={credentialsRef}
      onMfaEnabled={() => setMfaRequired(true)}
    />
  );
};
