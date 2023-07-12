import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProjectContextOptional } from 'tg.hooks/useProject';

export const useReportEvent = () => {
  const reportMutation = useApiMutation({
    url: '/v2/business-events/report',
    method: 'post',
  });

  const project = useProjectContextOptional()?.project;
  const preferredOrganization = usePreferredOrganization();
  const organizationId =
    project?.organizationOwner?.id ||
    preferredOrganization?.preferredOrganization?.id;

  return (
    eventName: string,
    data: Record<string, any> | undefined = undefined
  ) => {
    reportMutation.mutate({
      content: {
        'application/json': {
          eventName,
          data,
          projectId: project?.id,
          organizationId: organizationId,
        },
      },
    });
  };
};
