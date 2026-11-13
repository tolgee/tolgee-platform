import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import {
  SkipReason,
  useAiReviewSkipReasonLabel,
} from 'tg.ee.module/aiReview/runs/useAiReviewSkipReasonLabel';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useAiReviewRun } from 'tg.ee.module/aiReview/runs/useAiReviewRun';

type AiReviewRunRequest = components['schemas']['AiReviewRunRequest'];
type BatchJobModel = components['schemas']['BatchJobModel'];

export type AiReviewRunVariant = 'review' | 'score' | 'retry';

export const runVariantForMode = (
  mode: components['schemas']['AiReviewRuleModel']['mode']
): AiReviewRunVariant => (mode === 'SCORE_ONLY' ? 'score' : 'review');

type Props = {
  variant: AiReviewRunVariant;
  request: Partial<AiReviewRunRequest>;
  onClose: () => void;
  onStarted?: (job: BatchJobModel) => void;
};

const WARNING_LIMIT = 1000;

export const AiReviewRunDialog = ({
  variant,
  request,
  onClose,
  onStarted,
}: Props) => {
  const { t } = useTranslate();
  const { skipReasonLabel } = useAiReviewSkipReasonLabel();
  const { preview, start, projectId } = useAiReviewRun();

  const variantConfig = {
    review: {
      failedOnly: false,
      showsScoreOnlyRunLine: true,
      title: () =>
        t('ai_review_run_title_review', 'Review existing translations'),
      countLine: (count: number) =>
        t(
          'ai_review_run_to_review_count',
          '{count, plural, one {# translation will be reviewed} other {# translations will be reviewed}}',
          { count }
        ),
      startLabel: () => t('ai_review_run_start_review', 'Review now'),
    },
    score: {
      failedOnly: false,
      showsScoreOnlyRunLine: false,
      title: () =>
        t('ai_review_run_title_score', 'Score existing translations'),
      countLine: (count: number) =>
        t(
          'ai_review_run_to_score_count',
          '{count, plural, one {# translation will be scored} other {# translations will be scored}}',
          { count }
        ),
      startLabel: () => t('ai_review_run_start_score', 'Score now'),
    },
    retry: {
      failedOnly: true,
      showsScoreOnlyRunLine: true,
      title: () => t('ai_review_run_title_retry', 'Retry failed reviews'),
      countLine: (count: number) =>
        t(
          'ai_review_run_to_review_count',
          '{count, plural, one {# translation will be reviewed} other {# translations will be reviewed}}',
          { count }
        ),
      startLabel: () => t('ai_review_run_start_retry', 'Retry'),
    },
  } as const;
  const config = variantConfig[variant];

  const [reReview, setReReview] = useState(false);

  const fullRequest: AiReviewRunRequest = {
    languageIds: [],
    failedOnly: config.failedOnly,
    ...request,
    reReview,
  };

  useEffect(() => {
    preview.mutate({
      path: { projectId },
      content: { 'application/json': fullRequest },
    });
  }, [reReview]);

  const data = preview.data;
  const toReview = data?.toReview ?? 0;
  const scoreOnlyRunCount = data?.scoreOnlyRunCount ?? 0;
  const overriddenToReReview = data?.overriddenToReReview ?? 0;
  const skipped = Object.entries(data?.skipped ?? {}).filter(
    ([, count]) => Number(count) > 0
  );

  function handleStart() {
    start.mutate(
      {
        path: { projectId },
        content: { 'application/json': fullRequest },
      },
      {
        onSuccess(data) {
          onClose();
          onStarted?.(data);
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
      <DialogTitle>{config.title()}</DialogTitle>
      <DialogContent sx={{ display: 'grid', gap: 1, minWidth: 420 }}>
        {variant !== 'retry' && (
          <FormControlLabel
            control={
              <Checkbox
                size="small"
                checked={reReview}
                onChange={(_, checked) => setReReview(checked)}
                data-cy="ai-review-run-re-review"
              />
            }
            label={t(
              'ai_review_run_include_reviewed',
              'Include already AI reviewed'
            )}
          />
        )}
        {preview.isLoading || !data ? (
          <BoxLoading />
        ) : (
          <>
            <Typography
              data-cy="ai-review-run-to-review"
              data-cy-count={String(toReview)}
            >
              {config.countLine(toReview)}
            </Typography>
            {overriddenToReReview > 0 && !reReview && (
              <Typography
                variant="body2"
                color="textSecondary"
                data-cy="ai-review-run-includes-overridden"
              >
                {t(
                  'ai_review_run_includes_overridden',
                  '{count, plural, one {Includes # translation overridden by a human.} other {Includes # translations overridden by a human.}}',
                  { count: overriddenToReReview }
                )}
              </Typography>
            )}
            {config.showsScoreOnlyRunLine && scoreOnlyRunCount > 0 && (
              <Typography data-cy="ai-review-run-score-only-count">
                {t(
                  'ai_review_run_score_only_count',
                  '{count, plural, one {# of them will only be scored} other {# of them will only be scored}}',
                  { count: scoreOnlyRunCount }
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
                  'You are about to review more than {limit, number} translations.',
                  { limit: WARNING_LIMIT }
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
          {config.startLabel()}
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
