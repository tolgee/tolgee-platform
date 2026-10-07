import { useConfig } from 'tg.globalContext/helpers';
import {
  gitHubService,
  googleService,
  useOAuth2Service,
  OAuthService,
  OAuth2ServiceConfig,
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
  const completeOAuth2Config: OAuth2ServiceConfig | undefined =
    oauth2Config?.enabled &&
    oauth2Config.clientId &&
    oauth2Config.scopes &&
    oauth2Config.authorizationUrl
      ? {
          clientId: oauth2Config.clientId,
          authorizationUrl: oauth2Config.authorizationUrl,
          scopes: oauth2Config.scopes,
        }
      : undefined;
  const oauth2Service = useOAuth2Service(completeOAuth2Config);
  if (oauth2Service) {
    oAuthServices.push(oauth2Service);
  }
  return oAuthServices;
};
