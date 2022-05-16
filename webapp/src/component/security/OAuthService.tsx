import React from 'react';
import GitHubIcon from '@mui/icons-material/GitHub';
import GoogleIcon from '@mui/icons-material/Google';
import { LINKS, PARAMS } from 'tg.constants/links';

const GITHUB_BASE = 'https://github.com/login/oauth/authorize';
const GOOGLE_BASE = 'https://accounts.google.com/o/oauth2/v2/auth';

export interface OAuthService {
  authenticationUrl: string;
  buttonIcon: React.ReactElement;
  buttonLabelTranslationKey: string;
}

export const gitHubService = (clientId: string): OAuthService => {
  const redirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({
    [PARAMS.SERVICE_TYPE]: 'github',
  });
  return {
    authenticationUrl: encodeURI(
      `${GITHUB_BASE}?client_id=${clientId}&redirect_uri=${redirectUri}&scope=user:email`
    ),
    buttonIcon: <GitHubIcon />,
    buttonLabelTranslationKey: 'login_github_login_button',
  };
};

export const googleService = (clientId: string): OAuthService => {
  const redirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({
    [PARAMS.SERVICE_TYPE]: 'google',
  });
  return {
    authenticationUrl: encodeURI(
      `${GOOGLE_BASE}?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&scope=openid+email+https://www.googleapis.com/auth/userinfo.profile`
    ),
    buttonIcon: <GoogleIcon />,
    buttonLabelTranslationKey: 'login_google_login_button',
  };
};
