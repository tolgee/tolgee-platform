import { Alert, Box, Link, Popover, Tooltip, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { CheckCircleBroken, Stars01 } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { ShimmerText } from 'tg.component/ShimmerText';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';
import { useDateFormatter } from 'tg.hooks/useLocale';

import { AiReviewIssues } from 'tg.ee.module/aiReview/translations/AiReviewIssues';
import { AiReviewScoreTitle } from 'tg.ee.module/aiReview/translations/AiReviewScoreTitle';
import { useAiReviewDetails } from 'tg.ee.module/aiReview/translations/useAiReviewDetails';
import { AiReviewRetryButton } from 'tg.ee.module/aiReview/translations/AiReviewRetryButton';
import { useAiReviewStatusLabels } from 'tg.ee.module/aiReview/translations/useAiReviewStatusLabels';
import { isScoreOnlyStatus } from 'tg.ee.module/aiReview/translations/aiReviewVisibility';

type AiReviewSummary = components['schemas']['TranslationAiReviewSummaryModel'];
type State = components['schemas']['TranslationViewModel']['state'];

type Props = {
  anchorEl: HTMLElement;
  onClose: () => void;
  translationId: number;
  aiReview: AiReviewSummary;
  state?: State;
};

// The outcome lines are 14px; the rule line, the issues heading and the explanations 13px.
const OUTCOME = { fontSize: 14, lineHeight: '20px' } as const;
const SMALL = { fontSize: 13, lineHeight: '18px' } as const;
// The outcome icon is 16px with a 6px gap, so a second line starts under the outcome text.
const OUTCOME_INDENT = '22px';

export const AiReviewPopover = ({
  anchorEl,
  onClose,
  translationId,
  aiReview,
  state,
}: Props) => {
  const { t } = useTranslate();
  const project = useProject();
  const { satisfiesPermission } = useProjectPermissions();
  const canRun = satisfiesPermission('translations.batch-machine');
  const timeDistance = useTimeDistance();
  const formatDate = useDateFormatter();
  const { failureReasonLabel } = useAiReviewStatusLabels();

  const detailsLoadable = useAiReviewDetails(translationId);
  const details = detailsLoadable.data;

  const status = aiReview.status;
  const failed = status === 'FAILED';
  const pending = status === 'PENDING';
  const scoreOnly = isScoreOnlyStatus(status);
  const overridden = status === 'OVERRIDDEN';
  const reviewed = state === 'REVIEWED';
  // Below the threshold, but Translation protection keeps the translation Reviewed.
  const protectedReviewed = status === 'BELOW_THRESHOLD' && reviewed;
  const marked = status === 'APPROVED';
  const stays = status === 'BELOW_THRESHOLD' && !reviewed;
  const showRetry =
    !overridden && (failed || Boolean(details?.lastFailureReason)) && canRun;

  const ruleDeleted = details?.ruleId == null;
  const ruleName = ruleDeleted
    ? t('ai_review_popover_rule_name_deleted', 'Deleted rule')
    : details?.ruleName ?? '';

  const title = failed
    ? t('ai_review_popover_title_failed', 'AI review failed')
    : pending
    ? t('ai_review_popover_title_pending', 'AI review in progress')
    : t('ai_review_popover_title_score', 'AI review score');

  const formatFull = (time: number) =>
    formatDate(time, { dateStyle: 'medium', timeStyle: 'short' });

  // An override shows when the person changed it; otherwise when the AI reviewed it.
  const changedAt = overridden ? details?.resolvedAt : undefined;
  const shownAt = changedAt ?? details?.reviewedAt;

  const resolvedName =
    details?.resolvedBy &&
    (details.resolvedBy.name || details.resolvedBy.username);
  const overriddenText = reviewed
    ? resolvedName
      ? t('ai_review_popover_overridden_reviewed', 'Reviewed by {name}', {
          name: resolvedName,
        })
      : t(
          'ai_review_popover_overridden_reviewed_unknown',
          'Reviewed by a team member'
        )
    : resolvedName
    ? t(
        'ai_review_popover_overridden_translated',
        'Set back to translated by {name}',
        { name: resolvedName }
      )
    : t(
        'ai_review_popover_overridden_translated_unknown',
        'Set back to translated by a team member'
      );

  const issueCount = details?.issues.length ?? 0;

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
          sx: (theme) => {
            const tokens = theme.palette.tokens;
            // A left stripe in the colour of the translation state the review left behind:
            // green Reviewed, yellow Translated. None when it decided nothing (score only, failed, pending).
            const stripe =
              marked || protectedReviewed || (overridden && reviewed)
                ? tokens.state.reviewed
                : stays || overridden
                ? tokens.state.translated
                : 'transparent';
            return {
              borderRadius: 2,
              width: 340,
              maxWidth: '100%',
              overflow: 'hidden',
              borderLeft: `4px solid ${stripe}`,
            };
          },
        },
      }}
      data-cy="ai-review-popover"
    >
      {!details ? (
        <Box p={2}>
          <BoxLoading />
        </Box>
      ) : (
        <Box p={2} display="grid" gap={1}>
          {/* Title with the plain score on the right; the tooltip explains it. */}
          <AiReviewScoreTitle
            title={title}
            score={failed ? undefined : details.score}
            scoreTooltip={
              (stays || protectedReviewed) && details.threshold != null
                ? t(
                    'ai_review_popover_score_tooltip_needs',
                    'AI review score {score} of 100 · needs {threshold}+ to be marked as reviewed',
                    { score: details.score, threshold: details.threshold }
                  )
                : t(
                    'ai_review_popover_score_tooltip',
                    'AI review score {score} of 100',
                    { score: details.score }
                  )
            }
            titleProps={{ 'data-cy': 'ai-review-popover-status' }}
            scoreProps={{ 'data-cy': 'ai-review-popover-score-value' }}
          />

          {/* What happened to the translation. */}
          {failed && (
            <Alert severity="error" data-cy="ai-review-popover-failure-reason">
              {details.failureReason
                ? failureReasonLabel(details.failureReason)
                : t('ai_review_popover_status_failed', 'Review failed')}
            </Alert>
          )}
          {(marked || protectedReviewed || (overridden && reviewed)) && (
            <Box
              display="flex"
              alignItems="center"
              gap={0.75}
              sx={(theme) => ({
                ...OUTCOME,
                mt: -0.5,
                // Success green: the darker shade on white, the main (brighter) one in dark mode, so it stays readable.
                color:
                  theme.palette.mode === 'dark'
                    ? theme.palette.tokens.success.main
                    : theme.palette.tokens.success.dark,
                '& svg': { width: 16, height: 16, flexShrink: 0 },
              })}
              {...(overridden
                ? { 'data-cy': 'ai-review-popover-resolved' }
                : {
                    'data-cy': 'ai-review-popover-score',
                    'data-cy-with-threshold': 'true',
                  })}
            >
              <CheckCircleBroken />
              {overridden
                ? overriddenText
                : protectedReviewed
                ? t(
                    'ai_review_popover_outcome_stays_reviewed',
                    'Stays reviewed'
                  )
                : t(
                    'ai_review_popover_outcome_marked',
                    'Auto-marked as reviewed'
                  )}
            </Box>
          )}
          {protectedReviewed && (
            <Typography
              color="text.primary"
              sx={{ ...OUTCOME, mt: -0.75, pl: OUTCOME_INDENT }}
              data-cy="ai-review-popover-protected-note"
            >
              {details.threshold != null
                ? t(
                    'ai_review_popover_protected_reason',
                    'Below threshold {threshold}. Kept by Translation protection.',
                    { threshold: details.threshold }
                  )
                : t(
                    'ai_review_popover_protected_reason_plain',
                    'Kept by Translation protection.'
                  )}
            </Typography>
          )}
          {overridden && !reviewed && (
            <Typography
              color="text.primary"
              sx={{ ...OUTCOME, mt: -0.5 }}
              data-cy="ai-review-popover-resolved"
            >
              {overriddenText}
            </Typography>
          )}
          {stays && (
            <Typography
              color="text.primary"
              sx={{ ...OUTCOME, mt: -0.5 }}
              data-cy="ai-review-popover-score"
              data-cy-with-threshold="true"
            >
              {details.threshold != null
                ? t(
                    'ai_review_popover_outcome_stays',
                    'Stays translated (needs {threshold}+)',
                    { threshold: details.threshold }
                  )
                : t(
                    'ai_review_popover_outcome_stays_plain',
                    'Stays translated'
                  )}
            </Typography>
          )}
          {scoreOnly && (
            <Typography
              color="text.primary"
              sx={{ ...OUTCOME, mt: -0.5 }}
              data-cy="ai-review-popover-score-only"
            >
              {t('ai_review_popover_outcome_score_only', 'Score only')}
            </Typography>
          )}

          {/* Which rule (grey AI icon and name, pink on hover, opens the rules in a new window) and when.
              Not for a failed review: there is no result to explain. */}
          {!failed && (
            <Box
              display="flex"
              alignItems="center"
              justifyContent="space-between"
              gap={1.5}
              color="text.secondary"
              sx={{ ...SMALL, minWidth: 0 }}
            >
              <Tooltip
                title={
                  <Box display="grid" gap={0.25}>
                    <span data-cy="ai-review-popover-rule-name">
                      {t('ai_review_popover_rule_label', 'Rule: {name}', {
                        name: ruleName,
                      })}
                    </span>
                    {details.promptName && (
                      <span data-cy="ai-review-popover-prompt">
                        {t('ai_review_popover_prompt', 'Prompt: {name}', {
                          name: details.promptName,
                        })}
                      </span>
                    )}
                    {details.model && (
                      <span data-cy="ai-review-popover-model">
                        {t('ai_review_popover_model', 'Model: {name}', {
                          name: details.model,
                        })}
                      </span>
                    )}
                  </Box>
                }
              >
                {ruleDeleted ? (
                  <Box
                    component="span"
                    display="inline-flex"
                    alignItems="center"
                    gap={0.5}
                    sx={{ '& svg': { width: 14, height: 14, flexShrink: 0 } }}
                    data-cy="ai-review-popover-rule"
                    data-cy-rule="deleted"
                  >
                    <Stars01 />
                    {ruleName}
                  </Box>
                ) : (
                  <Link
                    href={LINKS.PROJECT_AI_REVIEW.build({
                      [PARAMS.PROJECT_ID]: project.id,
                    })}
                    target="_blank"
                    rel="noopener noreferrer"
                    underline="hover"
                    color="inherit"
                    sx={(theme) => ({
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 0.5,
                      minWidth: 0,
                      '& svg': { width: 14, height: 14, flexShrink: 0 },
                      '&:hover': { color: theme.palette.primary.main },
                    })}
                    data-cy="ai-review-popover-rule"
                    data-cy-rule={ruleName}
                  >
                    <Stars01 />
                    {ruleName}
                  </Link>
                )}
              </Tooltip>
              {shownAt != null && (
                <Tooltip
                  title={
                    changedAt != null
                      ? t('ai_review_popover_changed_at', 'Changed {date}', {
                          date: formatFull(changedAt),
                        })
                      : t('ai_review_popover_reviewed_at', 'Reviewed {date}', {
                          date: formatFull(shownAt),
                        })
                  }
                >
                  <span
                    style={{ cursor: 'default', whiteSpace: 'nowrap' }}
                    data-cy="ai-review-popover-reviewed"
                  >
                    {timeDistance(shownAt)}
                  </span>
                </Tooltip>
              )}
            </Box>
          )}

          {/* A newer review is running: Tolgee's shimmer text from the bulk AI operations, in grey. */}
          {aiReview.queued && !pending && (
            <Box
              mt={1}
              textAlign="center"
              sx={OUTCOME}
              data-cy="ai-review-popover-queued"
            >
              <ShimmerText
                sx={(theme) => {
                  // The shine is the lighter text token, so it shows in light and dark mode alike.
                  const grey = theme.palette.tokens.text.secondary;
                  const shine = theme.palette.tokens.text.tertiary;
                  return {
                    color: grey,
                    backgroundImage: `linear-gradient(90deg, ${grey} 0%, ${grey} 35%, ${shine} 50%, ${grey} 65%, ${grey} 100%)`,
                  };
                }}
              >
                {t(
                  'ai_review_popover_queued_running',
                  'New AI review running…'
                )}
              </ShimmerText>
            </Box>
          )}
          {/* Not when the review itself failed: that error is already shown above. */}
          {details.lastFailureReason && !overridden && !failed && (
            <Alert
              severity="error"
              sx={{ mt: 1 }}
              data-cy="ai-review-popover-re-review-failed"
            >
              {t(
                'ai_review_popover_re_review_failed_short',
                'Re-review failed. This is the previous result.'
              )}
            </Alert>
          )}

          {/* Point deductions below a full-width line, under "Issue" or "Issues" with the total from two. */}
          {!failed && issueCount > 0 && (
            <Box
              mt={1}
              mx={-2}
              px={2}
              pt={1.5}
              sx={(theme) => ({
                borderTop: `1px solid ${theme.palette.tokens.border.secondary}`,
              })}
            >
              <AiReviewIssues issues={details.issues} />
            </Box>
          )}

          {showRetry && (
            <AiReviewRetryButton
              translationId={translationId}
              onClose={onClose}
            />
          )}
        </Box>
      )}
    </Popover>
  );
};
