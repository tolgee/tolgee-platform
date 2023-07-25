import { useEffect, useRef, useState } from 'react';
import { useSelector } from 'react-redux';

import { createProvider } from 'tg.fixtures/createProvider';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import {
  BatchJobProgress,
  WebsocketClient,
} from 'tg.websocket-client/WebsocketClient';
import { AppState } from 'tg.store/index';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { GlobalError } from '../error/GlobalError';
import { useApiQuery } from '../service/http/useQueryApi';
import {
  BatchJobModel,
  BatchJobStatus,
} from 'tg.views/projects/translations/BatchOperations/types';

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
    const [connected, setConnected] = useState<boolean>();

    const [knownJobs, setKnownJobs] = useState<number[]>([]);

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
                };
              }) || []
            ).reverse()
          );
        },
      },
    });

    const [batchOperations, setBatchOperations] =
      useState<(Partial<BatchJobModel> & BatchJobUpdateModel)[]>();

    const jwtToken = useSelector(
      (state: AppState) => state.global.security.jwtToken
    );

    const changeHandler = ({ data }: BatchJobProgress) => {
      const exists = batchOperations?.find((job) => job.id === data.jobId);
      if (!exists) {
        if (!knownJobs.includes(data.jobId)) {
          // only refetch jobs first time we see unknown job
          setKnownJobs((jobs) => [...jobs, data.jobId]);
          setBatchOperations((jobs) => [
            ...(jobs || []),
            {
              id: data.jobId,
              progress: data.processed,
              totalItems: data.total,
              status: data.status,
            },
          ]);
          batchJobsLoadable.refetch();
        }
      } else {
        setBatchOperations((jobs) =>
          jobs?.map((job) => {
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
    };

    const changeHandlerRef = useRef(changeHandler);
    changeHandlerRef.current = changeHandler;

    useEffect(() => {
      if (jwtToken) {
        const client = WebsocketClient({
          authentication: { jwtToken: jwtToken },
          serverUrl: process.env.REACT_APP_API_URL,
          onConnected: () => setConnected(true),
          onConnectionClose: () => setConnected(false),
        });

        client.subscribe(`/projects/${id}/batch-job-progress`, (e) => {
          changeHandlerRef?.current(e);
        });
        return () => client.disconnect();
      }
    }, [id, jwtToken]);

    const { updatePreferredOrganization } = usePreferredOrganization();

    useEffect(() => {
      if (project.data?.organizationOwner) {
        updatePreferredOrganization(project.data.organizationOwner.id);
      }
    }, [project.data]);

    const isLoading = project.isLoading || settings.isLoading;

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
