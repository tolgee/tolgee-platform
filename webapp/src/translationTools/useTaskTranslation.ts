import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type TaskType = components['schemas']['TaskModel']['type'];

export function useTaskTypeTranslation() {
  const { t } = useTranslate();

  return (code: TaskType) => {
    switch (code) {
      case 'REVIEW':
        return t('task_type_review');
      case 'TRANSLATE':
        return t('task_type_translate');
      default:
        return code;
    }
  };
}
