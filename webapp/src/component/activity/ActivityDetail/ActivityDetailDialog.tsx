import { useState } from 'react';
import { Dialog, DialogContent, DialogProps } from '@mui/material';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { ActivityDetailContent } from './ActivityDetailContent';
import { ActivityModel } from '../types';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

type Props = DialogProps & {
  detailId: number;
  data: ActivityModel | undefined;
  initialDiffEnabled: boolean;
  onClose: () => void;
};

export const ActivityDetailDialog: React.FC<Props> = ({
  data,
  detailId,
  initialDiffEnabled,
  ...dialogProps
}) => {
  const project = useProject();
  const [diffEnabled, setDiffEnabled] = useState(initialDiffEnabled);
  const toggleDiff = () => {
    setDiffEnabled(!diffEnabled);
  };

  const detailLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/activity/revisions/{revisionId}',
    method: 'get',
    path: { projectId: project.id, revisionId: detailId },
    options: { enabled: !data },
  });

  const detailData = data || detailLoadable.data;

  return (
    <Dialog {...dialogProps} data-cy="activity-detail-dialog">
      {detailLoadable.isLoading ? (
        <DialogContent>
          <BoxLoading />
        </DialogContent>
      ) : (
        <ActivityDetailContent
          data={detailData!}
          onClose={dialogProps.onClose}
          onToggleDiff={toggleDiff}
          diffEnabled={diffEnabled}
        />
      )}
    </Dialog>
  );
};
