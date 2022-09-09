import { useTranslationsService } from './useTranslationsService';
import { useConfig } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';
import { useEffect } from 'react';
import {
  Modification,
  WebsocketClient,
} from '../../../../../websocket-client/WebsocketClient';

export const useWebsocketListener = (
  translationService: ReturnType<typeof useTranslationsService>
) => {
  const config = useConfig();
  const project = useProject();
  const jwtToken = useSelector(
    (state: AppState) => state.global.security.jwtToken
  );

  useEffect(() => {
    if (jwtToken) {
      const client = WebsocketClient({
        authentication: { jwtToken: jwtToken },
        serverUrl: process.env.REACT_APP_API_URL,
      });
      client.subscribe(
        `/projects/${project.id}/translation-data-modified`,
        (event) => {
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

          const keyUpdates = event.data.keys?.map((key) => ({
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
