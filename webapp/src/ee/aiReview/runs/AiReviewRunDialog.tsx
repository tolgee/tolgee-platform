import { useEffect } from 'react';
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useAiReviewRun } from './useAiReviewRun';

type AiReviewRunRequest = components['schemas']['AiReviewRunRequest'];
type SkipReason =
  keyof components['schemas']['AiReviewRunPreviewModel']['skipped'];

export type AiReviewRunVariant = 'review' | 'score' | 'retry';

type Props = {
  variant: AiReviewRunVariant;
  request: Partial<AiReviewRunRequest>;
  onClose: () => void;
};

const WARNING_LIMIT = 1000;

export const AiReviewRunDialog = ({ variant, request, onClose }: Props) => {
  const { t } = useTranslate();
  const { preview, start, projectId } = useAiReviewRun();

  const fullRequest: AiReviewRunRequest = {
    languageIds: [],
    reReview: false,
    failedOnly: variant === 'retry',
    ...request,
  };

  useEffect(() => {
    preview.mutate({
      path: { projectId },
      content: { 'application/json': fullRequest },
    });
  }, []);

  const data = preview.data;
  const toReview = data?.toReview ?? 0;
  const dryRunOnly = data?.dryRunOnly ?? 0;
  const skipped = Object.entries(data?.skipped ?? {}).filter(
    ([, count]) => Number(count) > 0
  );

  function skipReasonLabel(reason: SkipReason, count: number) {
    const params = { count };
    switch (reason) {
      case 'ALREADY_HAS_RESULT':
        return t(
          'ai_review_run_skip_already_has_result',
          'Already has a result: {count}',
          params
        );
      case 'ALREADY_QUEUED':
        return t(
          'ai_review_run_skip_already_queued',
          'Already queued: {count}',
          params
        );
      case 'CONFIRMED_BY_HUMAN':
        return t(
          'ai_review_run_skip_confirmed_by_human',
          'Confirmed by a human: {count}',
          params
        );
      case 'REJECTED_BY_HUMAN':
        return t(
          'ai_review_run_skip_rejected_by_human',
          'Rejected by a human: {count}',
          params
        );
      case 'HUMAN_REVIEWED':
        return t(
          'ai_review_run_skip_human_reviewed',
          'Reviewed by a human: {count}',
          params
        );
      case 'NO_RULE':
        return t(
          'ai_review_run_skip_no_rule',
          'No rule applies: {count}',
          params
        );
      case 'NEVER':
        return t(
          'ai_review_run_skip_never',
          'The rule says Never: {count}',
          params
        );
      case 'OPEN_QA_ISSUES':
        return t(
          'ai_review_run_skip_open_qa_issues',
          'Open QA issues: {count}',
          params
        );
      case 'OUTDATED':
        return t('ai_review_run_skip_outdated', 'Outdated: {count}', params);
      case 'EMPTY':
        return t('ai_review_run_skip_empty', 'Empty: {count}', params);
      case 'NO_PERMISSION':
        return t(
          'ai_review_run_skip_no_permission',
          'No permission: {count}',
          params
        );
      default:
        return `${reason}: ${count}`;
    }
  }

  function title() {
    if (variant === 'score') {
      return t('ai_review_run_title_score', 'Score existing translations');
    }
    if (variant === 'retry') {
      return t('ai_review_run_title_retry', 'Retry failed reviews');
    }
    return t('ai_review_run_title_review', 'Review existing translations');
  }

  function handleStart() {
    start.mutate(
      {
        path: { projectId },
        content: { 'application/json': fullRequest },
      },
      {
        onSuccess() {
          onClose();
        },
      }
    );
  }

  return (
    <Dialog
      open={true}
      onClose={onClose}
      data-cy="ai-review-run-dialog"
      data-cy-variant={variant}
    >
      <DialogTitle>{title()}</DialogTitle>
      <DialogContent sx={{ display: 'grid', gap: 1, minWidth: 420 }}>
        {preview.isLoading || !data ? (
          <BoxLoading />
        ) : (
          <>
            <Typography data-cy="ai-review-run-to-review">
              {variant === 'score'
                ? t(
                    'ai_review_run_to_score_count',
                    '{count} translations will be scored',
                    { count: toReview }
                  )
                : t(
                    'ai_review_run_to_review_count',
                    '{count} translations will be reviewed',
                    { count: toReview }
                  )}
            </Typography>
            {variant !== 'score' && dryRunOnly > 0 && (
              <Typography data-cy="ai-review-run-dry-run-only">
                {t(
                  'ai_review_run_dry_run_only_count',
                  '{count} of them will only be scored',
                  { count: dryRunOnly }
                )}
              </Typography>
            )}
            {skipped.length > 0 && (
              <Box display="grid">
                <Typography variant="caption" color="textSecondary">
                  {t('ai_review_run_skipped_heading', 'Skipped')}
                </Typography>
                {skipped.map(([reason, count]) => (
                  <Typography
                    key={reason}
                    variant="body2"
                    color="textSecondary"
                    data-cy="ai-review-run-skip-reason"
                    data-cy-reason={reason}
                  >
                    {skipReasonLabel(reason as SkipReason, Number(count))}
                  </Typography>
                ))}
              </Box>
            )}
            {toReview > WARNING_LIMIT && (
              <Alert severity="warning" data-cy="ai-review-run-warning">
                {t(
                  'ai_review_run_warning_large',
                  'You are about to review more than 1,000 translations.'
                )}
              </Alert>
            )}
          </>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} data-cy="ai-review-run-cancel">
          {t('ai_review_run_cancel', 'Cancel')}
        </Button>
        <LoadingButton
          variant="contained"
          color="primary"
          loading={start.isLoading}
          disabled={preview.isLoading || toReview === 0}
          onClick={handleStart}
          data-cy="ai-review-run-start"
        >
          {variant === 'score'
            ? t('ai_review_run_start_score', 'Score now')
            : variant === 'retry'
            ? t('ai_review_run_start_retry', 'Retry')
            : t('ai_review_run_start_review', 'Review now')}
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
