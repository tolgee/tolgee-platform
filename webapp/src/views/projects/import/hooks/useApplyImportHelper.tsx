import { useEffect, useState } from 'react';

import { useProject } from 'tg.hooks/useProject';

import { useImportDataHelper } from './useImportDataHelper';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { T } from '@tolgee/react';

export const useApplyImportHelper = (
  dataHelper: ReturnType<typeof useImportDataHelper>
) => {
  const [conflictNotResolvedDialogOpen, setConflictNotResolvedDialogOpen] =
    useState(false);

  const importApplyLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/import/apply',
    method: 'put',
  });

  const project = useProject();
  const error = importApplyLoadable.error;

  const message = useMessage();

  const onApplyImport = () => {
    const unResolvedCount = dataHelper.result?._embedded?.languages?.reduce(
      (acc, curr) => acc + curr.conflictCount - curr.resolvedCount,
      0
    );
    if (unResolvedCount === 0) {
      importApplyLoadable.mutate(
        {
          path: {
            projectId: project.id,
          },
          query: {},
        },
        {
          onSuccess() {
            dataHelper.refetchData();
            message.success(<T>import-successful-message</T>);
          },
        }
      );
      return;
    }
    setConflictNotResolvedDialogOpen(true);
  };

  useEffect(() => {
    const error = importApplyLoadable.error;
    if (error?.code == 'conflict_is_not_resolved') {
      setConflictNotResolvedDialogOpen(true);
      return;
    }
  }, [importApplyLoadable.error]);

  useEffect(() => {
    if (importApplyLoadable.isSuccess) {
      dataHelper.refetchData();
    }
  }, [importApplyLoadable.isSuccess]);

  const onDialogClose = () => {
    setConflictNotResolvedDialogOpen(false);
  };

  return {
    onDialogClose,
    onApplyImport,
    conflictNotResolvedDialogOpen,
    error,
    loading: importApplyLoadable.isLoading,
    loaded: importApplyLoadable.isSuccess,
  };
};
