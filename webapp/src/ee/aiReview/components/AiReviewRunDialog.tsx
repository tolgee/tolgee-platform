import { useState } from 'react';
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
  styled,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { getRunPreviewState } from 'tg.ee.module/aiReview/utils/runPreview';
import {
  AiReviewRunDialogSubject,
  useAiReviewRunDialogTexts,
} from 'tg.ee.module/aiReview/utils/useAiReviewRunDialogTexts';
import { AiReviewRunCountsTable } from 'tg.ee.module/aiReview/components/AiReviewRunCountsTable';

type BatchJobModel = components['schemas']['BatchJobModel'];
type AiReviewRunRequest = components['schemas']['AiReviewRunRequest'];

const StyledContent = styled(DialogContent)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(2)};
  width: 560px;
  max-width: 100%;
  box-sizing: border-box;
`;

type Props = AiReviewRunDialogSubject & {
  projectId: number;
  request: Omit<AiReviewRunRequest, 'rereview'>;
  dismissWhenNothingToReview?: boolean;
  onClose: () => void;
  onStarted: (job: BatchJobModel) => void;
};

export const AiReviewRunDialog = (props: Props) => {
  const { projectId, request, dismissWhenNothingToReview, onClose, onStarted } =
    props;
  const subject: AiReviewRunDialogSubject = props;
  const { t } = useTranslate();
  const texts = useAiReviewRunDialogTexts();
  const [rereview, setRereview] = useState(false);
  const ruleDryRun = subject.variant === 'rule' && subject.rule.dryRun;

  const previewLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/ai-review/run/preview',
    method: 'post',
    path: { projectId },
    content: { 'application/json': { ...request, rereview: false } },
    options: {
      cacheTime: 0,
      onSuccess(data) {
        if (
          dismissWhenNothingToReview &&
          data.withoutRereview.willReview === 0
        ) {
          onClose();
        }
      },
      onError() {
        onClose();
      },
    },
  });

  const startLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/run',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/ai-review/rules',
  });

  const preview = previewLoadable.data;

  if (!preview) {
    if (dismissWhenNothingToReview) {
      return null;
    }
    return (
      <Dialog open onClose={onClose} data-cy="ai-review-run-dialog">
        <StyledContent>
          <BoxLoading />
        </StyledContent>
      </Dialog>
    );
  }

  if (dismissWhenNothingToReview && preview.withoutRereview.willReview === 0) {
    return null;
  }

  const state = getRunPreviewState(preview, rereview);
  const {
    counts,
    willReview,
    skipped,
    rereviewAdds,
    nothingToReview,
    isLarge,
  } = state;
  const allDryRunOnly = counts.dryRunOnly === willReview;
  const showDryRunNote =
    counts.dryRunOnly > 0 && !(ruleDryRun && allDryRunOnly);

  function handleStart() {
    startLoadable.mutate(
      {
        path: { projectId },
        content: { 'application/json': { ...request, rereview } },
      },
      {
        onSuccess(job) {
          onStarted(job);
        },
      }
    );
  }

  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="md"
      data-cy="ai-review-run-dialog"
      data-cy-will-review={willReview}
      data-cy-variant={subject.variant}
      data-cy-dry-run={String(ruleDryRun)}
      data-cy-nothing-to-review={String(nothingToReview)}
    >
      <DialogTitle data-cy="ai-review-run-dialog-title">
        {texts.getTitle(subject, state)}
      </DialogTitle>
      <StyledContent>
        <Typography color="text.secondary">
          {texts.getDescription(subject, preview, state)}
        </Typography>
        {preview.total > 0 && (
          <AiReviewRunCountsTable
            counts={counts}
            willReview={willReview}
            skipped={skipped}
          />
        )}
        {rereviewAdds > 0 && (
          <Box>
            <FormControlLabel
              control={
                <Checkbox
                  checked={rereview}
                  onChange={(e) => setRereview(e.target.checked)}
                  data-cy="ai-review-run-rereview-checkbox"
                />
              }
              label={t(
                'ai_review_run_rereview',
                'Re-review translations that already have a result'
              )}
            />
            <Typography
              variant="body2"
              color="text.secondary"
              sx={{ pl: 4, mt: -0.5 }}
            >
              {nothingToReview
                ? t(
                    'ai_review_run_rereview_hint_would_add',
                    '{count, plural, one {Would add # translation.} other {Would add # translations.}}',
                    { count: rereviewAdds }
                  )
                : t(
                    'ai_review_run_rereview_hint',
                    '{count, plural, one {Adds # translation. Its current result is replaced.} other {Adds # translations. Their current result is replaced.}}',
                    { count: rereviewAdds }
                  )}
            </Typography>
          </Box>
        )}
        {isLarge && (
          <Alert severity="warning" data-cy="ai-review-run-large-warning">
            {t(
              'ai_review_run_large',
              'This is a large run. It reviews {count, number} translations and takes a while. You can cancel it any time.',
              { count: willReview }
            )}
          </Alert>
        )}
        {showDryRunNote && (
          <Alert severity="info" data-cy="ai-review-run-dry-run-note">
            {allDryRunOnly
              ? t(
                  'ai_review_run_dry_run_only_note',
                  'All of them fall under Dry run rules. They get a score, their state does not change.'
                )
              : t(
                  'ai_review_run_dry_run_note',
                  '{dryRun, number} of the {count, number} fall under Dry run rules. They get a score, their state does not change.',
                  { dryRun: counts.dryRunOnly, count: willReview }
                )}
          </Alert>
        )}
        {nothingToReview && counts.noRule > 0 && (
          <Typography color="text.secondary">
            {t(
              'ai_review_run_nothing_no_rule_hint',
              'To review keys that no rule covers, add a rule or widen one in AI settings.'
            )}
          </Typography>
        )}
      </StyledContent>
      <DialogActions>
        {nothingToReview ? (
          <Button color="primary" onClick={onClose} data-cy="ai-review-run-ok">
            {t('ai_review_run_ok', 'OK')}
          </Button>
        ) : (
          <>
            <Button onClick={onClose} data-cy="ai-review-run-cancel">
              {subject.variant === 'rule'
                ? t('ai_review_run_later', 'Later')
                : t('global_cancel_button')}
            </Button>
            <LoadingButton
              color="primary"
              loading={startLoadable.isLoading}
              onClick={handleStart}
              data-cy="ai-review-run-confirm"
            >
              {texts.getConfirmLabel(subject, state)}
            </LoadingButton>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
};
