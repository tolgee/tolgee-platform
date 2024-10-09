import { useState } from 'react';
import { useTranslate } from '@tolgee/react';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { messageService } from 'tg.service/MessageService';
import { Task } from 'tg.ee/task/components/taskSelect/types';
import { TaskSearchSelect } from 'tg.ee/task/components/taskSelect/TaskSearchSelect';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { OperationContainer } from './components/OperationContainer';
import { BatchOperationsSubmit } from './components/BatchOperationsSubmit';
import { OperationProps } from './types';

type Props = OperationProps;

export const OperationTaskAddKeys = ({ disabled, onFinished }: Props) => {
  const [task, setTask] = useState<Task | null>(null);
  const project = useProject();
  const { t } = useTranslate();

  const selection = useTranslationsSelector((c) => c.selection);

  const addTaskKeysLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}/keys',
    method: 'put',
  });

  function handleAddKeys() {
    addTaskKeysLoadable.mutate(
      {
        path: { projectId: project.id, taskNumber: task!.number },
        content: { 'application/json': { addKeys: selection } },
      },
      {
        onSuccess() {
          messageService.success(t('batch_operations_add_task_keys_success'));
          onFinished();
        },
      }
    );
  }

  return (
    <OperationContainer>
      <TaskSearchSelect
        label={null}
        value={task}
        onChange={(value) => setTask(value)}
        project={project}
        sx={{ width: 280 }}
      />
      <BatchOperationsSubmit
        disabled={disabled}
        onClick={handleAddKeys}
        loading={addTaskKeysLoadable.isLoading}
      />
    </OperationContainer>
  );
};
