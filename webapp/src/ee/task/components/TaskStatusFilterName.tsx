import { T } from '@tolgee/react';

import { TaskType } from 'tg.service/apiSchemaTypes';
import { TaskStatusFilter } from 'tg.views/projects/translations/TranslationFilters/tools';
import { useTaskTypeTranslation } from 'tg.translationTools/useTaskTranslation';

function StatusLabel({ status }: { status: TaskStatusFilter }) {
  switch (status) {
    case 'IN_OPEN_TASK':
      return <T keyName="translation_filters_in_open_task" />;
    case 'NOT_IN_OPEN_TASK':
      return <T keyName="translation_filters_not_in_open_task" />;
    case 'HAS_BEEN_IN_TASK':
      return <T keyName="translation_filters_has_task" />;
    case 'NEVER_IN_TASK':
      return <T keyName="translation_filters_has_no_task" />;
  }
}

export function TaskStatusFilterName({
  status,
  taskType,
}: {
  status: TaskStatusFilter;
  taskType?: TaskType;
}) {
  const translateTaskType = useTaskTypeTranslation();
  if (!taskType) {
    return <StatusLabel status={status} />;
  }
  return (
    <T
      keyName="translation_filters_task_type_status"
      params={{
        taskType: translateTaskType(taskType),
        status: <StatusLabel status={status} />,
      }}
    />
  );
}
