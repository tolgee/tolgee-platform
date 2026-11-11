import { Box } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

type Props = {
  translationId: number;
  onClose: () => void;
};

export const AiReviewRetryButton = ({ translationId, onClose }: Props) => {
  const { t } = useTranslate();
  const project = useProject();

  const retryMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/ai-review/retry',
    method: 'post',
  });

  const path = { projectId: project.id, translationId };

  return (
    <Box display="flex" justifyContent="end" gap={1}>
      <LoadingButton
        size="small"
        loading={retryMutation.isLoading}
        onClick={() =>
          retryMutation.mutate({ path }, { onSuccess: () => onClose() })
        }
        data-cy="ai-review-popover-retry"
      >
        {t('ai_review_popover_retry', 'Retry')}
      </LoadingButton>
    </Box>
  );
};
