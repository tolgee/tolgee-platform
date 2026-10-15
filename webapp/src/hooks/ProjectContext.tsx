import { useEffect, useRef, useState } from 'react';

import { createProvider } from 'tg.fixtures/createProvider';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { BatchJobProgress } from 'tg.websocket-client/WebsocketClient';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { GlobalError } from '../error/GlobalError';
import { useApiQuery } from '../service/http/useQueryApi';
import {
  BatchJobModel,
  BatchJobStatus,
} from 'tg.views/projects/translations/BatchOperations/types';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { DashboardPage } from 'tg.component/layout/DashboardPage';

type BatchJobUpdateModel = {
  totalItems: number;
  progress: number;
  status: BatchJobStatus;
  id: number;
};

type Props = {
  id: number;
};

export const [ProjectContext, useProjectActions, useProjectContext] =
  createProvider(({ id }: Props) => {
    const [knownJobs, setKnownJobs] = useState<number[]>([]);
    const client = useGlobalContext((c) => c.wsClient.client);
    const connected = useGlobalContext((c) => c.wsClient.clientConnected);

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

    const batchJobsLoadable = useApiQuery({
      url: '/v2/projects/{projectId}/current-batch-jobs',
      method: 'get',
      path: { projectId: id },
      options: {
        enabled: Boolean(connected),
        noGlobalLoading: true,
        staleTime: 0,
        onSuccess(data) {
          setBatchOperations(
            (
              data._embedded?.batchJobs?.map((job) => {
                // if data about the progress already exist, don't override them
                // because that can cause out of order issues
                const existingProgress = batchOperations?.find(
                  (o) => o.id === job.id
                );
                return {
                  ...job,
                  status: existingProgress?.status ?? job.status,
                  totalItems: existingProgress?.totalItems ?? job.totalItems,
                  progress: existingProgress?.progress ?? job.progress,
                  errorMessage:
                    existingProgress?.errorMessage ?? job.errorMessage,
                };
              }) || []
            ).reverse()
          );
        },
      },
    });

    const [batchOperations, setBatchOperations] =
      useState<(Partial<BatchJobModel> & BatchJobUpdateModel)[]>();

    const changeHandler = ({ data }: BatchJobProgress) => {
      const known = batchOperations?.find((job) => job.id === data.jobId);
      const firstSeen = !known && !knownJobs.includes(data.jobId);
      if (firstSeen) {
        setKnownJobs((jobs) => [...jobs, data.jobId]);
      }
      setBatchOperations((jobs) => {
        const existing = jobs?.find((job) => job.id === data.jobId);
        if (!existing) {
          return [
            ...(jobs || []),
            {
              id: data.jobId,
              progress: data.processed,
              totalItems: data.total,
              status: data.status,
              errorMessage: data.errorMessage,
            },
          ];
        }
        return jobs!.map((job) => {
          if (job.id !== data.jobId) {
            return job;
          }
          return {
            ...job,
            totalItems: data.total ?? job.totalItems,
            progress: data.processed ?? job.progress,
            status: data.status ?? job.status,
            errorMessage: data.errorMessage ?? job.errorMessage,
          };
        });
      });
      const turnedFailed =
        data.status === 'FAILED' && known?.status !== 'FAILED';
      if (firstSeen || turnedFailed) {
        batchJobsLoadable.refetch({ fetching: true });
      }
    };

    const changeHandlerRef = useRef(changeHandler);
    changeHandlerRef.current = changeHandler;

    useEffect(() => {
      if (client) {
        return client.subscribe(`/projects/${id}/batch-job-progress`, (e) => {
          changeHandlerRef?.current(e);
        });
      }
    }, [id, client]);

    const { updatePreferredOrganization } = usePreferredOrganization();

    useEffect(() => {
      if (project.data?.organizationOwner) {
        updatePreferredOrganization(project.data.organizationOwner.id);
      }
    }, [project.data]);

    const isLoading = project.isLoading || settings.isLoading;

    useGlobalLoading(isLoading);

    if (isLoading) {
      return <DashboardPage />;
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
      batchOperations: batchOperations?.filter((o) => o.type) as
        | BatchJobModel[]
        | undefined,
    };

    const actions = {
      refetchSettings: settings.refetch,
      refetchBatchJobs: batchJobsLoadable.refetch,
    };

    return [contextData, actions];
  });
