import { useEffect, useRef, useState } from 'react';

import { createProvider } from 'tg.fixtures/createProvider';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { BatchJobProgress } from 'tg.websocket-client/WebsocketClient';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { GlobalError } from '../error/GlobalError';
import { useApiQuery } from '../service/http/useQueryApi';
import { BatchJobModel } from 'tg.views/projects/translations/BatchOperations/types';
import {
  applyProgress,
  BatchJobState,
  mergeServerJobs,
} from 'tg.views/projects/translations/BatchOperations/OperationsSummary/utils';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { DashboardPage } from 'tg.component/layout/DashboardPage';

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
          setBatchOperations((jobs) =>
            mergeServerJobs(jobs, data._embedded?.batchJobs ?? [])
          );
        },
      },
    });

    const [batchOperations, setBatchOperations] = useState<BatchJobState[]>();

    const changeHandler = ({ data }: BatchJobProgress) => {
      setBatchOperations((jobs) => applyProgress(jobs, data));
      const isNewJob = !knownJobs.includes(data.jobId);
      if (isNewJob) {
        setKnownJobs((ids) => [...ids, data.jobId]);
      }
      // FAILED: load error message
      if (isNewJob || data.status === 'FAILED') {
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
