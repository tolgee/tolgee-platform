import { usePutTaskTranslation } from 'tg.service/TranslationHooks';
import { useProject } from 'tg.hooks/useProject';

import { SetTaskTranslationState } from '../types';
import { useTranslationsService } from './useTranslationsService';

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
};

export const useTaskService = ({ translations }: Props) => {
  const putTaskTranslation = usePutTaskTranslation();
  const project = useProject();

  const setTaskTranslationState = (data: SetTaskTranslationState) =>
    putTaskTranslation.mutate({
      path: {
        projectId: project.id,
        taskId: data.taskId,
        keyId: data.keyId,
      },
      content: {
        'application/json': {
          done: data.done,
        },
      },
    });

  return {
    setTaskTranslationState,
    isLoading: putTaskTranslation.isLoading,
  };
};
