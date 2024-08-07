import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type TaskType = components['schemas']['TaskModel']['state'];

export function useTaskStateTranslation() {
  const { t } = useTranslate();

  return (code: TaskType) => {
    switch (code) {
      case 'CLOSED':
        return t('task_state_closed');
      case 'DONE':
        return t('task_state_done');
      case 'IN_PROGRESS':
        return t('task_state_in_progress');
      default:
        return code;
    }
  };
}
