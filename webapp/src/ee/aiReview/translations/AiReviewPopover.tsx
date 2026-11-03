import { Box, Button, Divider, Popover, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';

import { AiReviewBadge } from './AiReviewBadge';
import { AiReviewIssueList } from './AiReviewIssueList';
import { useAiReviewDetails } from './useAiReviewDetails';

type AiReviewSummary = components['schemas']['TranslationAiReviewSummaryModel'];
type State = components['schemas']['TranslationViewModel']['state'];
type FailureReason = NonNullable<
  components['schemas']['TranslationAiReviewModel']['failureReason']
>;

type Props = {
  anchorEl: HTMLElement;
  onClose: () => void;
  translationId: number;
  aiReview: AiReviewSummary;
  state?: State;
  auto?: boolean;
  stateChangeEnabled?: boolean;
};

export const AiReviewPopover = ({
  anchorEl,
  onClose,
  translationId,
  aiReview,
  state,
  auto,
  stateChangeEnabled,
}: Props) => {
  const { t } = useTranslate();
  const project = useProject();
  const { satisfiesPermission } = useProjectPermissions();
  const canRun = satisfiesPermission('translations.batch-machine');
  const timeDistance = useTimeDistance();

  const detailsLoadable = useAiReviewDetails(translationId, true);
  const details = detailsLoadable.data;

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

  function statusLine() {
    switch (aiReview.status) {
      case 'APPROVED':
        return t('ai_review_popover_status_approved', 'Reviewed by AI');
      case 'BELOW_THRESHOLD':
        return t('ai_review_popover_status_below', 'Below threshold');
      case 'DRY_RUN_PASS':
        return t(
          'ai_review_popover_status_dry_run_pass',
          'Dry run: would be auto-reviewed'
        );
      case 'DRY_RUN_FAIL':
        return t(
          'ai_review_popover_status_dry_run_fail',
          'Dry run: would not be auto-reviewed'
        );
      case 'FAILED':
        return t('ai_review_popover_status_failed', 'Review failed');
      default:
        return aiReview.status;
    }
  }

  function failureReasonLabel(reason: FailureReason) {
    switch (reason) {
      case 'NO_PROVIDER':
        return t(
          'ai_review_failure_no_provider',
          'No AI provider is configured'
        );
      case 'PROVIDER_ERROR':
        return t(
          'ai_review_failure_provider_error',
          'The AI provider returned an error'
        );
      case 'OUT_OF_CREDITS':
        return t('ai_review_failure_out_of_credits', 'Out of AI credits');
      case 'TEMPLATE_ERROR':
        return t(
          'ai_review_failure_template_error',
          'The prompt template failed'
        );
      case 'PARSE_ERROR':
        return t(
          'ai_review_failure_parse_error',
          'The AI response could not be read'
        );
      default:
        return reason;
    }
  }

  const protectedNoteVisible =
    aiReview.status === 'BELOW_THRESHOLD' && state === 'REVIEWED' && auto;
  const showActions = aiReview.status === 'APPROVED' && stateChangeEnabled;
  const showRetry =
    (aiReview.status === 'FAILED' || Boolean(details?.lastFailureReason)) &&
    canRun;

  return (
    <Popover
      open={true}
      anchorEl={anchorEl}
      onClose={onClose}
      onClick={(e) => e.stopPropagation()}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      slotProps={{
        paper: {
          sx: { padding: 2, borderRadius: 2, minWidth: 320, maxWidth: 420 },
        },
      }}
      data-cy="ai-review-popover"
    >
      {!details ? (
        <BoxLoading />
      ) : (
        <Box display="grid" gap={1}>
          <Box display="flex" justifyContent="space-between" gap={1}>
            <Typography fontWeight={600} data-cy="ai-review-popover-status">
              {statusLine()}
            </Typography>
            <AiReviewBadge aiReview={aiReview} />
          </Box>
          {protectedNoteVisible && (
            <Typography
              variant="body2"
              color="textSecondary"
              data-cy="ai-review-popover-protected-note"
            >
              {t(
                'ai_review_popover_protected_note',
                'Still Reviewed because the project uses Translation protection.'
              )}
            </Typography>
          )}
          {details.score !== undefined && (
            <Typography variant="body2" data-cy="ai-review-popover-score">
              {t(
                'ai_review_popover_score',
                'Score {score} / 100, threshold {threshold}',
                {
                  score: details.score,
                  threshold: details.threshold ?? '—',
                }
              )}
            </Typography>
          )}
          <Box display="grid" color="text.secondary">
            <Typography
              variant="body2"
              data-cy="ai-review-popover-rule"
              data-cy-rule={details.ruleName ?? 'deleted'}
            >
              {details.ruleName
                ? t('ai_review_popover_rule', 'Rule: {name}', {
                    name: details.ruleName,
                  })
                : t('ai_review_popover_rule_deleted', 'Rule: deleted')}
            </Typography>
            {details.promptName && (
              <Typography variant="body2" data-cy="ai-review-popover-prompt">
                {t('ai_review_popover_prompt', 'Prompt: {name}', {
                  name: details.promptName,
                })}
              </Typography>
            )}
            {details.model && (
              <Typography variant="body2" data-cy="ai-review-popover-model">
                {t('ai_review_popover_model', 'Model: {name}', {
                  name: details.model,
                })}
              </Typography>
            )}
            {details.reviewedAt !== undefined && (
              <Typography variant="body2" data-cy="ai-review-popover-reviewed">
                {t('ai_review_popover_reviewed', 'Reviewed {time}', {
                  time: timeDistance(details.reviewedAt),
                })}
              </Typography>
            )}
          </Box>
          {details.lastFailureReason && (
            <Typography
              variant="body2"
              color="warning.main"
              data-cy="ai-review-popover-re-review-failed"
            >
              {t(
                'ai_review_popover_re_review_failed',
                'Re-review failed: {reason}. The previous result stays.',
                { reason: failureReasonLabel(details.lastFailureReason) }
              )}
            </Typography>
          )}
          <Divider />
          {aiReview.status === 'FAILED' ? (
            <Typography
              variant="body2"
              data-cy="ai-review-popover-failure-reason"
            >
              {details.failureReason
                ? failureReasonLabel(details.failureReason)
                : statusLine()}
            </Typography>
          ) : (
            <AiReviewIssueList issues={details.issues} />
          )}
          {(showActions || showRetry) && (
            <Box display="flex" justifyContent="end" gap={1}>
              {showRetry && (
                <LoadingButton
                  size="small"
                  loading={retryMutation.isLoading}
                  onClick={() =>
                    retryMutation.mutate(
                      { path },
                      { onSuccess: () => onClose() }
                    )
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
                      rejectMutation.mutate(
                        { path },
                        { onSuccess: () => onClose() }
                      )
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
                      confirmMutation.mutate(
                        { path },
                        { onSuccess: () => onClose() }
                      )
                    }
                    data-cy="ai-review-popover-confirm"
                  >
                    {t('ai_review_popover_confirm', 'Confirm review')}
                  </LoadingButton>
                </>
              )}
            </Box>
          )}
        </Box>
      )}
    </Popover>
  );
};
