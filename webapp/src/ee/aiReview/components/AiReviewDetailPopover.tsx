import { Box, Divider, Popover, styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { messageService } from 'tg.service/MessageService';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from 'tg.views/projects/translations/context/TranslationsContext';

import { useAiReviewOutcomeLabel } from 'tg.ee.module/aiReview/hooks/useAiReviewLabels';
import { useAiReviewRuleDisplayName } from 'tg.ee.module/aiReview/hooks/useAiReviewRuleDisplayName';
import { AiReviewIssuesList } from 'tg.ee.module/aiReview/components/AiReviewIssuesList';
import { AiReviewScoreChip } from 'tg.ee.module/aiReview/components/AiReviewScoreChip';

const StyledContent = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(1.5)};
  padding: ${({ theme }) => theme.spacing(2)};
  width: 400px;
  max-width: 90vw;
`;

const StyledHeader = styled('div')`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing(1.5)};
`;

const StyledMeta = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 2px ${({ theme }) => theme.spacing(1.5)};
  font-size: 13px;

  & .label {
    color: ${({ theme }) => theme.palette.text.secondary};
  }

  & .value {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;

const StyledActions = styled('div')`
  display: flex;
  justify-content: flex-end;
  gap: ${({ theme }) => theme.spacing(1)};
`;

type Props = {
  anchorEl: HTMLElement;
  onClose: () => void;
  projectId: number;
  translationId: number;
  keyId: number;
  languageTag: string;
  actionsEnabled: boolean;
};

export const AiReviewDetailPopover = ({
  anchorEl,
  onClose,
  projectId,
  translationId,
  keyId,
  languageTag,
  actionsEnabled,
}: Props) => {
  const { t } = useTranslate();
  const outcomeLabel = useAiReviewOutcomeLabel();
  const timeDistance = useTimeDistance();
  const { updateTranslation } = useTranslationsActions();
  const languages = useTranslationsSelector((c) => c.languages) ?? [];
  const getRuleDisplayName = useAiReviewRuleDisplayName();

  const detailLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/ai-review/translations/{translationId}',
    method: 'get',
    path: { projectId, translationId },
    fetchOptions: { disable404Redirect: true },
  });

  const confirmLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/translations/{translationId}/confirm',
    method: 'post',
  });

  const rejectLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/translations/{translationId}/reject',
    method: 'post',
  });

  const retryLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/ai-review/translations/{translationId}/retry',
    method: 'post',
  });

  function handleRetry() {
    retryLoadable.mutate(
      { path: { projectId, translationId } },
      {
        onSuccess() {
          updateTranslation({
            keyId,
            lang: languageTag,
            data: { aiReview: undefined },
          });
          messageService.success(
            <T
              keyName="ai_review_retry_success"
              defaultValue="AI review restarted"
            />
          );
          onClose();
        },
      }
    );
  }

  function handleConfirm() {
    confirmLoadable.mutate(
      { path: { projectId, translationId } },
      {
        onSuccess() {
          updateTranslation({
            keyId,
            lang: languageTag,
            data: { aiReview: undefined },
          });
          messageService.success(
            <T
              keyName="ai_review_confirm_success"
              defaultValue="Review confirmed"
            />
          );
          onClose();
        },
      }
    );
  }

  function handleReject() {
    rejectLoadable.mutate(
      { path: { projectId, translationId } },
      {
        onSuccess(data) {
          updateTranslation({
            keyId,
            lang: languageTag,
            data: { aiReview: undefined, state: data.state },
          });
          messageService.success(
            <T
              keyName="ai_review_reject_success"
              defaultValue="AI review rejected"
            />
          );
          onClose();
        },
      }
    );
  }

  const data = detailLoadable.data;
  const showActions =
    actionsEnabled && data?.outcome === 'AUTO_REVIEWED' && !data.dryRun;
  const showRetry = actionsEnabled && data?.outcome === 'FAILED';

  return (
    <Popover
      open
      anchorEl={anchorEl}
      onClose={onClose}
      onClick={(e) => e.stopPropagation()}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      transformOrigin={{ vertical: 'top', horizontal: 'right' }}
    >
      <StyledContent data-cy="ai-review-popover">
        {detailLoadable.isLoading || !data ? (
          detailLoadable.error ? (
            <Typography variant="body2" color="text.secondary">
              {t(
                'ai_review_popover_not_found',
                'AI review result is not available anymore.'
              )}
            </Typography>
          ) : (
            <BoxLoading />
          )
        ) : (
          <>
            <StyledHeader>
              <Typography
                variant="subtitle1"
                fontWeight={500}
                data-cy="ai-review-popover-status"
                data-cy-outcome={data.outcome}
              >
                {outcomeLabel(data.outcome, data.dryRun)}
              </Typography>
              <AiReviewScoreChip
                outcome={data.outcome}
                score={data.score}
                dryRun={data.dryRun}
                style={{ flexShrink: 0, marginTop: 3 }}
              />
            </StyledHeader>

            {data.score != null && (
              <Typography
                variant="body2"
                data-cy="ai-review-popover-score"
                data-cy-score={data.score}
                data-cy-threshold={data.threshold ?? ''}
              >
                {data.threshold != null
                  ? t(
                      'ai_review_popover_score_threshold',
                      'Score {score} / 100, threshold {threshold}',
                      { score: data.score, threshold: data.threshold }
                    )
                  : t(
                      'ai_review_popover_score_never',
                      'Score {score} / 100, never auto-reviewed in this language',
                      { score: data.score }
                    )}
              </Typography>
            )}

            {data.outcome === 'FAILED' && data.errorCode && (
              <Box
                sx={{ fontSize: 14, color: 'error.main' }}
                data-cy="ai-review-popover-error"
              >
                <TranslatedError code={data.errorCode} />
                {data.errorDetail && (
                  <Box
                    sx={(theme) => ({
                      color: 'text.secondary',
                      fontSize: theme.typography.caption.fontSize,
                      mt: 0.5,
                    })}
                  >
                    {data.errorDetail}
                  </Box>
                )}
              </Box>
            )}

            <StyledMeta>
              <span className="label">
                {t('ai_review_popover_rule', 'Rule')}
              </span>
              <span className="value" data-cy="ai-review-popover-rule">
                {data.ruleScope
                  ? getRuleDisplayName(
                      { name: data.ruleName, ...data.ruleScope },
                      languages
                    )
                  : t('ai_review_popover_rule_deleted', 'Deleted rule')}
              </span>
              <span className="label">
                {t('ai_review_popover_prompt', 'Prompt')}
              </span>
              <span className="value">
                {data.promptName ||
                  t('ai_review_default_prompt', 'Default review prompt')}
              </span>
              {data.providerName && (
                <>
                  <span className="label">
                    {t('ai_review_popover_model', 'Model')}
                  </span>
                  <span className="value">{data.providerName}</span>
                </>
              )}
              {data.reviewedAt != null && (
                <>
                  <span className="label">
                    {t('ai_review_popover_time', 'Reviewed')}
                  </span>
                  <span className="value">{timeDistance(data.reviewedAt)}</span>
                </>
              )}
            </StyledMeta>

            {data.outcome !== 'FAILED' && (
              <>
                <Divider />
                <AiReviewIssuesList issues={data.issues} />
              </>
            )}

            {showRetry && (
              <>
                <Divider />
                <StyledActions>
                  <LoadingButton
                    size="small"
                    variant="outlined"
                    loading={retryLoadable.isLoading}
                    onClick={handleRetry}
                    data-cy="ai-review-retry"
                  >
                    {t('ai_review_retry', 'Retry review')}
                  </LoadingButton>
                </StyledActions>
              </>
            )}

            {showActions && (
              <>
                <Divider />
                <StyledActions>
                  <LoadingButton
                    size="small"
                    variant="outlined"
                    loading={rejectLoadable.isLoading}
                    disabled={confirmLoadable.isLoading}
                    onClick={handleReject}
                    data-cy="ai-review-reject"
                  >
                    {t('ai_review_reject', 'Reject')}
                  </LoadingButton>
                  <LoadingButton
                    size="small"
                    variant="contained"
                    color="primary"
                    loading={confirmLoadable.isLoading}
                    disabled={rejectLoadable.isLoading}
                    onClick={handleConfirm}
                    data-cy="ai-review-confirm"
                  >
                    {t('ai_review_confirm', 'Confirm review')}
                  </LoadingButton>
                </StyledActions>
              </>
            )}
          </>
        )}
      </StyledContent>
    </Popover>
  );
};
