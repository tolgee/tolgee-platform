import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProjectContextOptional } from './useProject';
import { AnonymousIdService } from 'tg.service/AnonymousIdService';
import { useEffect } from 'react';
import { container } from 'tsyringe';
import { TokenService } from 'tg.service/TokenService';

export const useReportEvent = () => {
  const reportMutation = useApiMutation({
    url: '/v2/public/business-events/report',
    method: 'post',
  });
  const isAuthenticated = container.resolve(TokenService).getToken() !== null;
  const storedPreferredOrganization = usePreferredOrganization();

  const preferredOrganization = isAuthenticated
    ? storedPreferredOrganization
    : undefined;

  const storedProject = useProjectContextOptional()?.project;
  const project = isAuthenticated ? storedProject : undefined;

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
          anonymousUserId: AnonymousIdService.get() || undefined,
        },
      },
    });
  };
};

export const useReportOnce = (
  eventName: string,
  data: Record<string, any> | undefined = undefined
) => {
  const reportEvent = useReportEvent();

  useEffect(() => {
    reportEvent(eventName, data);
  }, []);
};
