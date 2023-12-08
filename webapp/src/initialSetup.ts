import { LanguageStorageMiddleware, TolgeePlugin } from '@tolgee/react';
import { QueryClient } from 'react-query';
import { apiSchemaHttpService } from 'tg.service/http/ApiSchemaHttpService';
import { tokenService } from 'tg.service/TokenService';

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

// store language in both localStorage and on server
// so user get consistent experience even when he's signed out
const storageMiddleware: LanguageStorageMiddleware = {
  async getLanguage() {
    const response = await queryClient.fetchQuery(
      ['/v2/public/initial-data', null, null],
      () =>
        apiSchemaHttpService.schemaRequest('/v2/public/initial-data', 'get')({})
    );
    return (
      response.languageTag || localStorage.getItem(LANGUAGE_KEY) || undefined
    );
  },
  async setLanguage(languageTag) {
    localStorage.setItem(LANGUAGE_KEY, languageTag);
    if (tokenService.getToken()) {
      apiSchemaHttpService.schemaRequest(
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
