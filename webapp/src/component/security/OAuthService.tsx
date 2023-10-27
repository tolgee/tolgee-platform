import React from 'react';
import GitHubIcon from '@mui/icons-material/GitHub';
import GoogleIcon from '@mui/icons-material/Google';
import LoginIcon from '@mui/icons-material/Login';
import { LINKS, PARAMS } from 'tg.constants/links';
import { T } from '@tolgee/react';
import { v4 as uuidv4 } from 'uuid';

const GITHUB_BASE = 'https://github.com/login/oauth/authorize';
const GOOGLE_BASE = 'https://accounts.google.com/o/oauth2/v2/auth';
const LOCAL_STORAGE_STATE_KEY = 'oauth2State';

export interface OAuthService {
  id: string;
  authenticationUrl: string;
  buttonIcon: React.ReactElement;
  loginButtonTitle: React.ReactElement;
  signUpButtonTitle: React.ReactElement;
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
    buttonIcon: <GitHubIcon />,
    loginButtonTitle: <T keyName="login_github_login_button" />,
    signUpButtonTitle: <T keyName="login_github_signup_button" />,
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
    buttonIcon: <GoogleIcon />,
    loginButtonTitle: <T keyName="login_google_login_button" />,
    signUpButtonTitle: <T keyName="login_google_signup_button" />,
  };
};

export const oauth2Service = (
  clientId: string,
  authorizationUrl: string,
  scopes: string[] = []
): OAuthService => {
  const state = uuidv4();
  localStorage.setItem(LOCAL_STORAGE_STATE_KEY, state);
  const redirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({
    [PARAMS.SERVICE_TYPE]: 'oauth2',
  });
  const authUrl = new URL(authorizationUrl);
  authUrl.searchParams.set('client_id', clientId);
  authUrl.searchParams.set('redirect_uri', redirectUri);
  authUrl.searchParams.set('response_type', 'code');
  authUrl.searchParams.set('scope', scopes.join(' '));
  authUrl.searchParams.set('state', state);
  return {
    id: 'oauth2',
    authenticationUrl: authUrl.toString(),
    buttonIcon: <LoginIcon />,
    loginButtonTitle: <T keyName="login_oauth2_login_button" />,
    signUpButtonTitle: <T keyName="login_oauth2_signup_button" />,
  };
};
