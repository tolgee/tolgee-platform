import { useTranslationsService } from './useTranslationsService';
import { useConfig } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';
import { useEffect } from 'react';
import {
  Modification,
  TranslationsModifiedData,
  WebsocketClient,
} from 'tg.websocket-client/WebsocketClient';

export const useWebsocketListener = (
  translationService: ReturnType<typeof useTranslationsService>
) => {
  const config = useConfig();
  const project = useProject();
  const jwtToken = useSelector(
    (state: AppState) => state.global.security.jwtToken
  );

  useEffect(() => {
    function handler(event: TranslationsModifiedData) {
      const translationUpdates = event.data?.translations?.map(
        (translation) => ({
          keyId: translation.relations.key.entityId,
          language: translation.relations.language.data.tag,
          value: getModifyingObject(translation.modifications),
        })
      );

      if (translationUpdates) {
        translationService.changeTranslations(translationUpdates);
      }

      const keyUpdates = event.data?.keys?.map((key) => ({
        keyId: key.id,
        value:
          key.changeType == 'DEL'
            ? { deleted: true }
            : {
                ...getModifyingObject(key.modifications, {
                  name: 'keyName',
                }),
              },
      }));

      if (keyUpdates) {
        translationService.updateTranslationKeys(keyUpdates);
      }
    }

    if (jwtToken) {
      const client = WebsocketClient({
        authentication: { jwtToken: jwtToken },
        serverUrl: process.env.REACT_APP_API_URL,
      });
      client.subscribe(
        `/projects/${project.id}/translation-data-modified`,
        (event) => {
          // arbitrary delay, so the socket event is not faster
          // than response of http request
          setTimeout(() => handler(event), 300);
        }
      );
      return () => client.disconnect();
    }
  }, [config, project, jwtToken]);
};

const getModifyingObject = (
  value: Record<string, Modification<any>>,
  fieldMapping?: Record<string, string>
) => {
  const result = {};
  Object.entries(value).forEach(([field, modification]) => {
    result[fieldMapping?.[field] || field] = modification.new;
  });
  return result;
};
