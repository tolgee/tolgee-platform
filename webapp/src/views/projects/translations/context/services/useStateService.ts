import { container } from 'tsyringe';
import { useTranslate } from '@tolgee/react';

import { usePutTranslationState } from 'tg.service/TranslationHooks';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useProject } from 'tg.hooks/useProject';
import { MessageService } from 'tg.service/MessageService';

import { SetTranslationState } from '../types';
import { useTranslationsService } from './useTranslationsService';

const messaging = container.resolve(MessageService);

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
};

export const useStateService = ({ translations }: Props) => {
  const putTranslationState = usePutTranslationState();
  const t = useTranslate();

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
          parsed.forEach((error) => messaging.error(t(error)));
        },
      }
    );

  return { changeState, isLoading: putTranslationState.isLoading };
};
