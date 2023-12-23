import { useEffect, useState } from 'react';

import { useProject } from 'tg.hooks/useProject';

import { useImportDataHelper } from './useImportDataHelper';
import { useNdJsonStreamedMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { T } from '@tolgee/react';
import { OperationStatusType } from '../component/ImportFileInput';

export const useApplyImportHelper = (
  dataHelper: ReturnType<typeof useImportDataHelper>
) => {
  const [conflictNotResolvedDialogOpen, setConflictNotResolvedDialogOpen] =
    useState(false);

  const [status, setStatus] = useState(
    undefined as OperationStatusType | undefined
  );

  const importApplyMutation = useNdJsonStreamedMutation({
    url: '/v2/projects/{projectId}/import/apply',
    method: 'put',
    fetchOptions: {
      // error is displayed on the page
      disableErrorNotification: true,
    },
    onData(data) {
      setStatus(data.status);
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
          onSuccess() {
            dataHelper.refetchData();
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
