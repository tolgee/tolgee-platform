import { useTheme } from '@mui/material';

import { BatchJobProgress } from 'tg.websocket-client/WebsocketClient';
import { BatchJobModel, BatchJobStatus } from '../types';

export function useStatusColor() {
  const { palette } = useTheme();

  return (status: BatchJobStatus) => {
    switch (status) {
      case 'FAILED':
        return palette.error.main;
      case 'SUCCESS':
        return palette.success.main;
      default:
        return palette.text.secondary;
    }
  };
}

export const END_STATUSES: BatchJobStatus[] = [
  'SUCCESS',
  'FAILED',
  'CANCELLED',
];

export const isFinished = (status: BatchJobStatus | undefined) =>
  !!status && END_STATUSES.includes(status);

export type BatchJobState = Partial<BatchJobModel> & {
  id: number;
  totalItems: number;
  progress: number;
  status: BatchJobStatus;
};

export function applyProgress(
  jobs: BatchJobState[] = [],
  data: BatchJobProgress['data']
): BatchJobState[] {
  const job = jobs.find((j) => j.id === data.jobId);
  if (!job) {
    return [
      ...jobs,
      {
        id: data.jobId,
        progress: data.processed,
        totalItems: data.total,
        status: data.status,
        errorMessage: data.errorMessage,
      },
    ];
  }
  if (isFinished(job.status)) {
    return jobs;
  }
  return jobs.map((j) =>
    j.id === data.jobId
      ? {
          ...j,
          totalItems: data.total ?? j.totalItems,
          progress: data.processed ?? j.progress,
          status: data.status ?? j.status,
          errorMessage: data.errorMessage ?? j.errorMessage,
        }
      : j
  );
}

export function mergeServerJobs(
  jobs: BatchJobState[] = [],
  serverJobs: BatchJobModel[]
): BatchJobState[] {
  return serverJobs
    .map((serverJob) => {
      const job = jobs.find((j) => j.id === serverJob.id);
      // live progress is newer than the server list,
      // unless the server already says the job is finished
      if (!job || isFinished(serverJob.status)) {
        return serverJob;
      }
      return {
        ...serverJob,
        status: job.status,
        totalItems: job.totalItems,
        progress: job.progress,
        errorMessage: job.errorMessage ?? serverJob.errorMessage,
      };
    })
    .reverse();
}

export const STATIC_STATUSES: BatchJobStatus[] = [
  'FAILED',
  'SUCCESS',
  'CANCELLED',
  'PENDING',
];

export const CANCELLABLE_STATUSES: BatchJobStatus[] = ['PENDING', 'RUNNING'];
