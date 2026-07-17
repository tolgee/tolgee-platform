import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { downloadBlobAsFile } from 'tg.fixtures/downloadResponseAsFile';

export type TaskModel = components['schemas']['TaskModel'];
export type TaskState = TaskModel['state'];

function toFileName(label: string) {
  return label.replace(/\s+/g, '_').toLowerCase();
}

export const taskReportFileName = (task: TaskModel) =>
  `${toFileName(task.name || `task_${task.number}`)}_report.xlsx`;

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
          const data = await (result as unknown as Response).blob();
          downloadBlobAsFile(data, taskReportFileName(task));
        },
      }
    );
  }
  return { downloadReport, isLoading: reportMutation.isLoading };
};
