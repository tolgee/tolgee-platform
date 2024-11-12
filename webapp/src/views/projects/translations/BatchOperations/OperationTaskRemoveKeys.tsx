import { useEffect, useState } from 'react';
import { useTranslate } from '@tolgee/react';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { messageService } from 'tg.service/MessageService';
import { Task } from 'tg.ee/task/components/taskSelect/types';
import { TextField } from 'tg.component/common/TextField';
import { TaskLabel } from 'tg.ee/task/components/TaskLabel';
import { FakeInput } from 'tg.component/FakeInput';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { OperationContainer } from './components/OperationContainer';
import { BatchOperationsSubmit } from './components/BatchOperationsSubmit';
import { OperationProps } from './types';

type Props = OperationProps;

export const OperationTaskRemoveKeys = ({ disabled, onFinished }: Props) => {
  const filteredTask = useTranslationsSelector((c) => c.prefilter?.task);
  const [task, setTask] = useState<Task | null>(null);
  const project = useProject();
  const { t } = useTranslate();

  const taskLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}',
    method: 'get',
    path: { projectId: project.id, taskNumber: filteredTask! },
    options: {
      enabled: typeof filteredTask === 'number',
      onSuccess(data) {
        setTask(data);
      },
      refetchOnMount: false,
    },
  });

  useEffect(() => {
    if (taskLoadable.data) {
      setTask((task) => task ?? taskLoadable.data);
    }
  }, [taskLoadable.data]);

  const selection = useTranslationsSelector((c) => c.selection);

  const addTaskKeysLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}/keys',
    method: 'put',
  });

  function handleAddKeys() {
    addTaskKeysLoadable.mutate(
      {
        path: { projectId: project.id, taskNumber: task!.number },
        content: { 'application/json': { removeKeys: selection } },
      },
      {
        onSuccess() {
          messageService.success(
            t('batch_operations_remove_task_keys_success')
          );
          onFinished();
        },
      }
    );
  }

  return (
    <OperationContainer>
      <TextField
        label={null}
        minHeight={false}
        sx={{ width: 280 }}
        disabled
        InputProps={{
          inputComponent: FakeInput,
          value: task && <TaskLabel task={task} />,
        }}
      />
      <BatchOperationsSubmit
        disabled={disabled}
        onClick={handleAddKeys}
        loading={addTaskKeysLoadable.isLoading}
      />
    </OperationContainer>
  );
};
