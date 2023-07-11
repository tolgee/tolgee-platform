import { useEffect, useState } from 'react';
import { GlobalError } from '../error/GlobalError';
import { useApiQuery } from '../service/http/useQueryApi';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { createProvider } from 'tg.fixtures/createProvider';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { WebsocketClient } from 'tg.websocket-client/WebsocketClient';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';

type Props = {
  id: number;
};

export const [ProjectContext, useProjectActions, useProjectContext] =
  createProvider(({ id }: Props) => {
    const project = useApiQuery({
      url: '/v2/projects/{projectId}',
      method: 'get',
      path: { projectId: id },
    });

    const settings = useApiQuery({
      url: '/v2/projects/{projectId}/machine-translation-service-settings',
      method: 'get',
      path: { projectId: id },
    });

    const batchOperationsLoadable = useApiQuery({
      url: '/v2/projects/{projectId}/batch-jobs',
      method: 'get',
      path: { projectId: id },
      query: { size: 100 },
      options: {
        onSuccess(data) {
          setBatchOperations(data._embedded?.batchJobs || []);
        },
      },
    });

    const [batchOperations, setBatchOperations] = useState(
      batchOperationsLoadable.data?._embedded?.batchJobs || []
    );

    const jwtToken = useSelector(
      (state: AppState) => state.global.security.jwtToken
    );

    useEffect(() => {
      if (jwtToken) {
        const client = WebsocketClient({
          authentication: { jwtToken: jwtToken },
          serverUrl: process.env.REACT_APP_API_URL,
        });
        client.subscribe(`/projects/${id}/batch-job-progress`, ({ data }) => {
          const exists = batchOperations.find((job) => job.id === data.jobId);
          if (!exists) {
            // job is not on the list, refetch the list
            batchOperationsLoadable.refetch();
          } else {
            setBatchOperations((jobs) =>
              jobs.map((job) => {
                if (job.id === data.jobId) {
                  return {
                    ...job,
                    totalItems: data.total ?? job.totalItems,
                    progress: data.processed ?? job.progress,
                    status: data.status ?? job.status,
                  };
                }
                return job;
              })
            );
          }
        });
        return () => client.disconnect();
      }
    }, [project, jwtToken]);

    const { updatePreferredOrganization } = usePreferredOrganization();

    useEffect(() => {
      if (project.data?.organizationOwner) {
        updatePreferredOrganization(project.data.organizationOwner.id);
      }
    }, [project.data]);

    const isLoading =
      project.isLoading ||
      settings.isLoading ||
      batchOperationsLoadable.isLoading;

    useGlobalLoading(isLoading);

    if (isLoading) {
      return null;
    }

    if (project.error || settings.error) {
      throw new GlobalError(
        'Unexpected error occurred',
        project.error?.code || settings.error?.code || 'Loadable error'
      );
    }

    const contextData = {
      project: project.data,
      enabledMtServices: settings.data?._embedded?.languageConfigs,
      batchOperations: batchOperations,
    };

    const actions = {
      refetchSettings: settings.refetch,
    };

    return [contextData, actions];
  });
