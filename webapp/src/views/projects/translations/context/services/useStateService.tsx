import { container } from 'tsyringe';
import { T } from '@tolgee/react';

import {
  usePutTranslationOutdated,
  usePutTranslationState,
} from 'tg.service/TranslationHooks';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useProject } from 'tg.hooks/useProject';
import { MessageService } from 'tg.service/MessageService';

import { SetTranslationOutdated, SetTranslationState } from '../types';
import { useTranslationsService } from './useTranslationsService';

const messaging = container.resolve(MessageService);

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
};

export const useStateService = ({ translations }: Props) => {
  const putTranslationState = usePutTranslationState();
  const putTranslationOutdated = usePutTranslationOutdated();
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
          translations.changeTranslations([
            { keyId: data.keyId, language: data.language, value: response },
          ]);
        },
        onError(e) {
          const parsed = parseErrorResponse(e);
          parsed.forEach((error) => messaging.error(<T>{error}</T>));
        },
      }
    );

  const changeOutdated = (data: SetTranslationOutdated) =>
    putTranslationOutdated.mutate(
      {
        path: {
          projectId: project.id,
          translationId: data.translationId,
          state: data.outdated,
        },
      },
      {
        onSuccess(response) {
          translations.changeTranslations([
            { keyId: data.keyId, language: data.language, value: response },
          ]);
        },
        onError(e) {
          const parsed = parseErrorResponse(e);
          parsed.forEach((error) => messaging.error(<T>{error}</T>));
        },
      }
    );

  return {
    changeState,
    changeOutdated,
    isLoading:
      putTranslationState.isLoading || putTranslationOutdated.isLoading,
  };
};
