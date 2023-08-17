import { usePutTranslationState } from 'tg.service/TranslationHooks';
import { useProject } from 'tg.hooks/useProject';

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
          translations.changeTranslations([
            { keyId: data.keyId, language: data.language, value: response },
          ]);
        },
      }
    );

  return {
    changeState,
    isLoading: putTranslationState.isLoading,
  };
};
