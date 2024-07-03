import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];
export type TaskModel = components['schemas']['TaskModel'];
export type TaskState = TaskModel['state'];

export const getTaskRedirect = (
  project: SimpleProjectModel,
  taskNumber: number
) => {
  return `${LINKS.GO_TO_PROJECT_TASK.build({
    [PARAMS.PROJECT_ID]: project.id,
  })}?number=${taskNumber}`;
};

function toFileName(label: string) {
  return label.replace(/[\s]+/g, '_').toLowerCase();
}

export const useTaskReport = () => {
  const reportMutation = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}/xlsx-report',
    method: 'get',
    fetchOptions: {
      rawResponse: true,
    },
  });

  function downloadReport(projectId: number, task: TaskModel) {
    reportMutation.mutate(
      {
        path: { projectId: projectId, taskNumber: task.number },
      },
      {
        async onSuccess(result) {
          const res = result as unknown as Response;
          const data = await res.blob();
          const url = URL.createObjectURL(data);
          const a = document.createElement('a');
          a.download = `${toFileName(task.name)}_report.xlsx`;
          a.href = url;
          a.click();
        },
      }
    );
  }
  return { downloadReport, isLoading: reportMutation.isLoading };
};

export const TASK_ACTIVE_STATES = ['NEW', 'IN_PROGRESS'] as TaskState[];
