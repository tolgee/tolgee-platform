import { Box, Divider, Popover, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';

import { AiReviewBadge } from 'tg.ee.module/aiReview/translations/AiReviewBadge';
import { AiReviewIssueList } from 'tg.ee.module/aiReview/translations/AiReviewIssueList';
import { useAiReviewDetails } from 'tg.ee.module/aiReview/translations/useAiReviewDetails';
import { AiReviewPopoverActions } from 'tg.ee.module/aiReview/translations/AiReviewPopoverActions';
import { useAiReviewStatusLabels } from 'tg.ee.module/aiReview/translations/useAiReviewStatusLabels';

type AiReviewSummary = components['schemas']['TranslationAiReviewSummaryModel'];
type State = components['schemas']['TranslationViewModel']['state'];

type Props = {
  anchorEl: HTMLElement;
  onClose: () => void;
  translationId: number;
  aiReview: AiReviewSummary;
  state?: State;
  stateChangeEnabled?: boolean;
};

type RuleRef =
  | { kind: 'deleted' }
  | { kind: 'unnamed' }
  | { kind: 'named'; name: string };

function describeReviewRule(details: {
  ruleId?: number;
  ruleName?: string;
}): RuleRef {
  if (details.ruleId == null) {
    return { kind: 'deleted' };
  }
  if (details.ruleName == null) {
    return { kind: 'unnamed' };
  }
  return { kind: 'named', name: details.ruleName };
}

function ruleLine(ref: RuleRef, t: ReturnType<typeof useTranslate>['t']) {
  switch (ref.kind) {
    case 'deleted':
      return t('ai_review_popover_rule_deleted', 'Rule: deleted');
    case 'unnamed':
      return t('ai_review_popover_rule_unnamed', 'Rule: unnamed rule');
    case 'named':
      return t('ai_review_popover_rule', 'Rule: {name}', { name: ref.name });
  }
}

export const AiReviewPopover = ({
  anchorEl,
  onClose,
  translationId,
  aiReview,
  state,
  stateChangeEnabled,
}: Props) => {
  const { t } = useTranslate();
  const { satisfiesPermission } = useProjectPermissions();
  const canRun = satisfiesPermission('translations.batch-machine');
  const timeDistance = useTimeDistance();
  const { statusLabel, failureReasonLabel } = useAiReviewStatusLabels();

  const detailsLoadable = useAiReviewDetails(translationId);
  const details = detailsLoadable.data;
  const rule = details && describeReviewRule(details);
  const resolvedParams =
    details?.resolvedBy && details.resolvedAt != null
      ? {
          name: details.resolvedBy.name ?? details.resolvedBy.username,
          time: timeDistance(details.resolvedAt),
        }
      : undefined;

  const protectedNoteVisible =
    aiReview.status === 'BELOW_THRESHOLD' && state === 'REVIEWED';
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
            <Typography fontWeight={500} data-cy="ai-review-popover-status">
              {statusLabel(aiReview.status)}
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
          {details.score != null && (
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
              data-cy-rule={rule?.kind === 'named' ? rule.name : rule?.kind}
            >
              {rule && ruleLine(rule, t)}
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
            {resolvedParams && (
              <Typography variant="body2" data-cy="ai-review-popover-resolved">
                {aiReview.status === 'REJECTED'
                  ? t(
                      'ai_review_popover_rejected_by',
                      'Rejected by {name} {time}',
                      resolvedParams
                    )
                  : t(
                      'ai_review_popover_confirmed_by',
                      'Confirmed by {name} {time}',
                      resolvedParams
                    )}
              </Typography>
            )}
            {details.reviewedAt != null && (
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
                : statusLabel(aiReview.status)}
            </Typography>
          ) : (
            <AiReviewIssueList issues={details.issues} />
          )}
          <AiReviewPopoverActions
            translationId={translationId}
            showRetry={showRetry}
            showActions={Boolean(showActions)}
            onClose={onClose}
          />
        </Box>
      )}
    </Popover>
  );
};
