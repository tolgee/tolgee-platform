import React, { useState } from 'react';
import { GitHub, Google } from 'tg.component/CustomIcons';
import { LogIn01 } from '@untitled-ui/icons-react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { T } from '@tolgee/react';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

const GITHUB_BASE = 'https://github.com/login/oauth/authorize';
const GOOGLE_BASE = 'https://accounts.google.com/o/oauth2/v2/auth';

export interface OAuthService {
  id: string;
  authenticationUrl: string;
  buttonIcon: React.ReactElement;
  loginButtonTitle: React.ReactElement;
  signUpButtonTitle: React.ReactElement;
  connectButtonTitle: React.ReactElement;
  disconnectButtonTitle: React.ReactElement;
}

export const gitHubService = (clientId: string): OAuthService => {
  const redirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({
    [PARAMS.SERVICE_TYPE]: 'github',
  });
  return {
    id: 'github',
    authenticationUrl: encodeURI(
      `${GITHUB_BASE}?client_id=${clientId}&redirect_uri=${redirectUri}&scope=user:email`
    ),
    buttonIcon: <GitHub width={20} height={20} />,
    loginButtonTitle: <T keyName="login_github_login_button" />,
    signUpButtonTitle: <T keyName="login_github_signup_button" />,
    connectButtonTitle: <T keyName="login_github_connect_button" />,
    disconnectButtonTitle: <T keyName="login_github_disconnect_button" />,
  };
};

export const googleService = (clientId: string): OAuthService => {
  const redirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({
    [PARAMS.SERVICE_TYPE]: 'google',
  });
  return {
    id: 'google',
    authenticationUrl: encodeURI(
      `${GOOGLE_BASE}?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&scope=openid+email+https://www.googleapis.com/auth/userinfo.profile`
    ),
    buttonIcon: <Google width={20} height={20} />,
    loginButtonTitle: <T keyName="login_google_login_button" />,
    signUpButtonTitle: <T keyName="login_google_signup_button" />,
    connectButtonTitle: <T keyName="login_google_connect_button" />,
    disconnectButtonTitle: <T keyName="login_google_disconnect_button" />,
  };
};

export const oauth2Service = (
  clientId: string,
  authorizationUrl: string,
  scopes: string[] = []
): OAuthService => {
  const { generateOAuthStateKey } = useGlobalActions();
  const [authenticationUrl] = useState(() => {
    const state = generateOAuthStateKey();
    const redirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({
      [PARAMS.SERVICE_TYPE]: 'oauth2',
    });
    const authUrl = new URL(authorizationUrl);
    authUrl.searchParams.set('client_id', clientId);
    authUrl.searchParams.set('redirect_uri', redirectUri);
    authUrl.searchParams.set('response_type', 'code');
    authUrl.searchParams.set('scope', scopes.join(' '));
    authUrl.searchParams.set('state', state);
    return authUrl.toString();
  });

  return {
    id: 'oauth2',
    authenticationUrl,
    buttonIcon: <LogIn01 />,
    loginButtonTitle: <T keyName="login_oauth2_login_button" />,
    signUpButtonTitle: <T keyName="login_oauth2_signup_button" />,
    connectButtonTitle: <T keyName="login_oauth2_connect_button" />,
    disconnectButtonTitle: <T keyName="login_oauth2_disconnect_button" />,
  };
};
