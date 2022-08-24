import { useConfig } from 'tg.globalContext/helpers';
import {
  gitHubService,
  googleService,
  oauth2Service,
  OAuthService,
} from 'tg.component/security/OAuthService';

export const useOAuthServices = () => {
  const remoteConfig = useConfig();

  const oAuthServices: OAuthService[] = [];
  const githubConfig = remoteConfig.authMethods?.github;
  const googleConfig = remoteConfig.authMethods?.google;
  const oauth2Config = remoteConfig.authMethods?.oauth2;
  if (githubConfig?.enabled && githubConfig.clientId) {
    oAuthServices.push(gitHubService(githubConfig.clientId));
  }
  if (googleConfig?.enabled && googleConfig.clientId) {
    oAuthServices.push(googleService(googleConfig.clientId));
  }
  if (
    oauth2Config?.enabled &&
    oauth2Config?.clientId &&
    oauth2Config.scopes &&
    oauth2Config?.authorizationUrl
  ) {
    oAuthServices.push(
      oauth2Service(
        oauth2Config.clientId,
        oauth2Config.authorizationUrl,
        oauth2Config.scopes
      )
    );
  }
  return oAuthServices;
};
