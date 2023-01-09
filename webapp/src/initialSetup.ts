import { LanguageStorageMiddleware, TolgeePlugin } from '@tolgee/react';
import { QueryClient } from 'react-query';
import { ApiSchemaHttpService } from 'tg.service/http/ApiSchemaHttpService';
import { TokenService } from 'tg.service/TokenService';
import { container } from 'tsyringe';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
      retry: false,
    },
  },
});

const LANGUAGE_KEY = '__tolgee_currentLanguage';

const apiHttpService = container.resolve(ApiSchemaHttpService);
const tokenService = container.resolve(TokenService);

// store language in both localStorage and on server
// so user get consistent experience even when he's signed out
const storageMiddleware: LanguageStorageMiddleware = {
  async getLanguage() {
    const response = await queryClient.fetchQuery(
      ['/v2/public/initial-data', null, null],
      () => apiHttpService.schemaRequest('/v2/public/initial-data', 'get')({})
    );
    return (
      response.languageTag || localStorage.getItem(LANGUAGE_KEY) || undefined
    );
  },
  async setLanguage(languageTag) {
    localStorage.setItem(LANGUAGE_KEY, languageTag);
    if (tokenService.getToken()) {
      apiHttpService.schemaRequest(
        '/v2/user-preferences/set-language/{languageTag}',
        'put'
      )({ path: { languageTag } });
    }
  },
};

export const languageStorage: TolgeePlugin = (tolgee, tools) => {
  tools.setLanguageStorage(storageMiddleware);
  return tolgee;
};
