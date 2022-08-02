import { T } from '@tolgee/react';

import { usePutTranslationState } from 'tg.service/TranslationHooks';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useProject } from 'tg.hooks/useProject';
import { messageService } from 'tg.service/MessageService';

import { SetTranslationState } from '../types';
import { useTranslationsService } from './useTranslationsService';

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
};

export const useStateService = ({ translations }: Props) => {
  const putTranslationState = usePutTranslationState();
  const project = useProject();

  const changeState = (data: SetTranslationState) =>
    putTranslationState.mutate(
      {
        path: {
          projectId: project.id,
          translationId: data.translationId,
          state: data.state,
        },
      },
      {
        onSuccess(response) {
          translations.changeTranslation(data.keyId, data.language, response);
        },
        onError(e) {
          const parsed = parseErrorResponse(e);
          parsed.forEach((error) => messageService.error(<T>{error}</T>));
        },
      }
    );

  return { changeState, isLoading: putTranslationState.isLoading };
};
