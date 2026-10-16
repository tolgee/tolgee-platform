import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useNumberFormatter } from 'tg.hooks/useLocale';
import { components } from 'tg.service/apiSchema.generated';

type AiReviewRunCounts = components['schemas']['AiReviewRunCounts'];

const StyledTable = styled('div')`
  display: grid;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: ${({ theme }) => theme.shape.borderRadius * 2}px;
  overflow: hidden;
`;

const StyledGroupRow = styled('div')`
  display: flex;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing(2)};
  padding: ${({ theme }) => theme.spacing(1.25, 2)};
  background: ${({ theme }) => theme.palette.tokens.background.hover};
  font-weight: 500;
  font-variant-numeric: tabular-nums;

  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

const StyledReasonRow = styled('div')`
  display: flex;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing(2)};
  padding: ${({ theme }) => theme.spacing(0.75, 2, 0.75, 3)};
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 15px;
  font-variant-numeric: tabular-nums;

  &:last-of-type {
    padding-bottom: ${({ theme }) => theme.spacing(1.25)};
  }
`;

type Props = {
  counts: AiReviewRunCounts;
  willReview: number;
  skipped: number;
};

export const AiReviewRunCountsTable = ({
  counts,
  willReview,
  skipped,
}: Props) => {
  const { t } = useTranslate();
  const formatNumber = useNumberFormatter();

  const reasons: { key: string; label: string; count: number }[] = [
    {
      key: 'noRule',
      label: t('ai_review_run_skipped_no_rule', 'No enabled rule matches'),
      count: counts.noRule,
    },
    {
      key: 'humanReviewed',
      label: t(
        'ai_review_run_skipped_human_reviewed',
        'Already reviewed by a human'
      ),
      count: counts.humanReviewed,
    },
    {
      key: 'alreadyHasResult',
      label: t(
        'ai_review_run_skipped_has_result',
        'Already has an AI review result'
      ),
      count: counts.alreadyHasResult,
    },
    {
      key: 'openQaIssues',
      label: t(
        'ai_review_run_skipped_open_qa',
        'Open QA issues (rule skips these)'
      ),
      count: counts.openQaIssues,
    },
    {
      key: 'never',
      label: t(
        'ai_review_run_skipped_never',
        'Language set to Never in the rule'
      ),
      count: counts.never,
    },
  ].filter((reason) => reason.count > 0);

  return (
    <StyledTable data-cy="ai-review-run-counts">
      {willReview > 0 && (
        <StyledGroupRow
          data-cy="ai-review-run-will-review"
          data-cy-count={willReview}
        >
          <span>{t('ai_review_run_will_review', 'Will be reviewed')}</span>
          <span>{formatNumber(willReview)}</span>
        </StyledGroupRow>
      )}
      {skipped > 0 && (
        <StyledGroupRow data-cy="ai-review-run-skipped" data-cy-count={skipped}>
          <span>{t('ai_review_run_skipped', 'Skipped')}</span>
          <span>{formatNumber(skipped)}</span>
        </StyledGroupRow>
      )}
      {reasons.map((reason) => (
        <StyledReasonRow
          key={reason.key}
          data-cy="ai-review-run-skipped-reason"
          data-cy-reason={reason.key}
          data-cy-count={reason.count}
        >
          <span>{reason.label}</span>
          <span>{formatNumber(reason.count)}</span>
        </StyledReasonRow>
      ))}
    </StyledTable>
  );
};
