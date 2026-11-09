import { Box, Button } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

type Props = {
  translationId: number;
  showRetry: boolean;
  showActions: boolean;
  onClose: () => void;
};

export const AiReviewPopoverActions = ({
  translationId,
  showRetry,
  showActions,
  onClose,
}: Props) => {
  const { t } = useTranslate();
  const project = useProject();

  const confirmMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/ai-review/confirm',
    method: 'put',
  });
  const rejectMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/ai-review/reject',
    method: 'put',
  });
  const retryMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/ai-review/retry',
    method: 'post',
  });

  const path = { projectId: project.id, translationId };

  if (!showActions && !showRetry) {
    return null;
  }

  return (
    <Box display="flex" justifyContent="end" gap={1}>
      {showRetry && (
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
      )}
      {showActions && (
        <>
          <Button
            size="small"
            disabled={rejectMutation.isLoading}
            onClick={() =>
              rejectMutation.mutate({ path }, { onSuccess: () => onClose() })
            }
            data-cy="ai-review-popover-reject"
          >
            {t('ai_review_popover_reject', 'Reject')}
          </Button>
          <LoadingButton
            size="small"
            variant="contained"
            color="primary"
            loading={confirmMutation.isLoading}
            onClick={() =>
              confirmMutation.mutate({ path }, { onSuccess: () => onClose() })
            }
            data-cy="ai-review-popover-confirm"
          >
            {t('ai_review_popover_confirm', 'Confirm review')}
          </LoadingButton>
        </>
      )}
    </Box>
  );
};
