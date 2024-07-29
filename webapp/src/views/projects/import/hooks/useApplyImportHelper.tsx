import { useEffect, useState } from 'react';

import { useProject } from 'tg.hooks/useProject';

import { useImportDataHelper } from './useImportDataHelper';
import { useNdJsonStreamedMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { T } from '@tolgee/react';
import { OperationStatusType } from '../component/ImportFileInput';
import { ApiError } from 'tg.service/http/ApiError';
import { errorAction } from 'tg.service/http/errorAction';

export const useApplyImportHelper = (
  dataHelper: ReturnType<typeof useImportDataHelper>
) => {
  const [conflictNotResolvedDialogOpen, setConflictNotResolvedDialogOpen] =
    useState(false);

  const [status, setStatus] = useState(
    undefined as OperationStatusType | undefined
  );

  const importApplyMutation = useNdJsonStreamedMutation({
    url: '/v2/projects/{projectId}/import/apply-streaming',
    method: 'put',
    fetchOptions: {
      // error is displayed on the page
      disableErrorNotification: true,
      disableAutoErrorHandle: true,
    },
    onData(data) {
      if (data.status == 'ERROR') {
        errorAction(data.errorResponseBody.code);
        throw new ApiError(data.errorResponseBody.code, data.errorResponseBody);
      }
      return setStatus(data.status);
    },
  });

  const project = useProject();
  const error = importApplyMutation.error;

  const message = useMessage();

  const onApplyImport = () => {
    const unResolvedCount = dataHelper.result?._embedded?.languages?.reduce(
      (acc, curr) => acc + curr.conflictCount - curr.resolvedCount,
      0
    );
    if (unResolvedCount === 0) {
      importApplyMutation.mutate(
        {
          path: {
            projectId: project.id,
          },
          query: {},
        },
        {
          onSuccess(data) {
            dataHelper.refetchData();
            const timeout = !data.find((d) => d.status === 'DONE');
            if (timeout) {
              message.error(<T keyName="import-timeout-message" />);
              return;
            }
            message.success(<T keyName="import-successful-message" />);
          },
        }
      );
      return;
    }
    setConflictNotResolvedDialogOpen(true);
  };

  useEffect(() => {
    const error = importApplyMutation.error;
    if (error?.code == 'conflict_is_not_resolved') {
      setConflictNotResolvedDialogOpen(true);
      return;
    }
  }, [importApplyMutation.error]);

  useEffect(() => {
    if (importApplyMutation.isSuccess) {
      dataHelper.refetchData();
    }
  }, [importApplyMutation.isSuccess]);

  const onDialogClose = () => {
    setConflictNotResolvedDialogOpen(false);
  };

  const clear = () => {
    importApplyMutation.reset();
    setStatus(undefined);
  };

  return {
    onDialogClose,
    onApplyImport,
    conflictNotResolvedDialogOpen,
    error,
    loading: importApplyMutation.isLoading,
    loaded: importApplyMutation.isSuccess,
    status,
    clear,
  };
};
