import { T, useTolgee, useTranslate } from '@tolgee/react';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useBatchOperationTypeTranslate } from 'tg.translationTools/useBatchOperationTypeTranslation';

import { PrefilterContainer } from './ContainerPrefilter';
import { Box } from '@mui/material';
import { useStatusColor } from '../BatchOperations/OperationsSummary/utils';
import { useBatchOperationStatusTranslate } from 'tg.translationTools/useBatchOperationStatusTranslate';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';

type Props = {
  jobId: number;
};

export const PrefilterFailedJob = ({ jobId }: Props) => {
  const tolgee = useTolgee(['language']);
  const project = useProject();
  const { t } = useTranslate();
  const translateType = useBatchOperationTypeTranslate();
  const getStatusColor = useStatusColor();
  const getStatusLabel = useBatchOperationStatusTranslate();

  const { data } = useApiQuery({
    url: '/v2/projects/{projectId}/batch-jobs/{id}',
    method: 'get',
    path: { projectId: project.id, id: jobId },
  });

  if (!data) {
    return null;
  }

  return (
    <PrefilterContainer
      title={<T keyName="failed_job_filter_indicator_label" />}
      content={
        <Box display="flex" gap={1} fontSize={14} fontWeight="700">
          <Box>
            {Intl.DateTimeFormat(tolgee.getLanguage(), {
              timeStyle: 'short',
            }).format(data.updatedAt)}
          </Box>

          <Box>{translateType(data.type)}</Box>

          <Box>
            {t('batch_operation_progress', {
              totalItems: data.totalItems,
              progress: data.progress,
            })}
          </Box>

          <Box color={getStatusColor(data.status)}>
            {getStatusLabel(data.status)}
          </Box>

          <Box>
            {data.author && (
              <AvatarImg
                owner={{
                  avatar: data.author.avatar,
                  id: data.author.id,
                  name: data.author.name,
                  type: 'USER',
                }}
                size={20}
              />
            )}
          </Box>
        </Box>
      }
    />
  );
};
