import { useState } from 'react';
import { useUser } from 'tg.globalContext/helpers';

import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { CANCELLABLE_STATUSES } from './utils';

type BatchJobModel = components['schemas']['BatchJobModel'];

type Props = {
  operation: BatchJobModel;
};

export function useOperationCancel({ operation }: Props) {
  const project = useProject();
  const [canceled, setCanceled] = useState(false);

  const user = useUser();
  const permissions = useProjectPermissions();
  const canCancelJobs = permissions.satisfiesPermission('batch-jobs.cancel');

  const cancelLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/batch-jobs/{id}/cancel',
    method: 'put',
  });

  function handleCancel() {
    setCanceled(true);
    cancelLoadable.mutate({
      path: { projectId: project.id, id: operation.id },
    });
  }

  const cancelable =
    CANCELLABLE_STATUSES.includes(operation.status) &&
    (canCancelJobs || operation.author?.id === user?.id);

  return {
    cancelable,
    loading: canceled,
    handleCancel,
  };
}
