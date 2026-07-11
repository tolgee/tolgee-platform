import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type TaskType = components['schemas']['TaskModel']['state'];

export function useTaskStateTranslation() {
  const { t } = useTranslate();

  return (code: TaskType) => {
    switch (code) {
      case 'CANCELED':
        return t('task_state_canceled');
      case 'FINISHED':
        return t('task_state_finished');
      case 'IN_PROGRESS':
        return t('task_state_in_progress');
      case 'NEW':
        return t('task_state_new');
      default:
        return code;
    }
  };
}
