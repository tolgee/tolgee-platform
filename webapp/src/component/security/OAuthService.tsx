import React from 'react';
import GitHubIcon from '@mui/icons-material/GitHub';
import GoogleIcon from '@mui/icons-material/Google';
import LoginIcon from '@mui/icons-material/Login';
import { LINKS, PARAMS } from 'tg.constants/links';

const GITHUB_BASE = 'https://github.com/login/oauth/authorize';
const GOOGLE_BASE = 'https://accounts.google.com/o/oauth2/v2/auth';

export interface OAuthService {
  id: string;
  authenticationUrl: string;
  buttonIcon: React.ReactElement;
  loginButtonTitle: string;
  signUpButtonTitle: string;
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
    loginButtonTitle: 'login_github_login_button',
    signUpButtonTitle: 'login_github_signup_button',
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
    loginButtonTitle: 'login_google_login_button',
    signUpButtonTitle: 'login_google_signup_button',
  };
};

export const oauth2Service = (
  clientId: string,
  authorizationUrl: string,
  scopes: string[] = []
): OAuthService => {
  const redirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({
    [PARAMS.SERVICE_TYPE]: 'oauth2',
  });
  return {
    id: 'oauth2',
    authenticationUrl: encodeURI(
      `${authorizationUrl}?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&scope=${scopes
        .map((scope) => `${scope}`)
        .join('+')}`
    ),
    buttonIcon: <LoginIcon />,
    loginButtonTitle: 'login_oauth2_login_button',
    signUpButtonTitle: 'login_oauth2_signup_button',
  };
};
