import { TaskType } from 'tg.service/apiSchemaTypes';
import { useTaskTypeTranslation } from 'tg.translationTools/useTaskTranslation';

export function TaskTypeFilterName({ taskType }: { taskType: TaskType }) {
  const label = useTaskTypeTranslation()(taskType);
  return <>{label}</>;
}
