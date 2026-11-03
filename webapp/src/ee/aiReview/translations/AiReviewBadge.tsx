import clsx from 'clsx';
import { alpha, styled } from '@mui/material';
import { AlertCircle, AlertTriangle, Stars01 } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';

type AiReviewSummary = components['schemas']['TranslationAiReviewSummaryModel'];

const StyledPill = styled('span')`
  display: inline-flex;
  align-items: center;
  gap: 2px;
  border-radius: 10px;
  padding: 3px 6px;
  font-size: 10px;
  font-weight: 600;
  line-height: 1;

  & svg {
    width: 12px;
    height: 12px;
  }

  &.green {
    background: ${({ theme }) => alpha(theme.palette.success.main, 0.15)};
    color: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? theme.palette.success.light
        : theme.palette.success.dark};
  }
  &.orange {
    background: ${({ theme }) => alpha(theme.palette.warning.main, 0.18)};
    color: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? theme.palette.warning.light
        : theme.palette.warning.dark};
  }
  &.grey {
    background: ${({ theme }) => alpha(theme.palette.text.secondary, 0.12)};
    color: ${({ theme }) => theme.palette.text.secondary};
  }
  &.dryRun {
    background: transparent;
    border: 1px solid;
  }
`;

type Props = {
  aiReview: AiReviewSummary;
};

export const AiReviewBadge = ({ aiReview }: Props) => {
  const { status, score, lastFailed } = aiReview;

  const look = clsx({
    green: status === 'APPROVED' || status === 'DRY_RUN_PASS',
    orange: status === 'BELOW_THRESHOLD' || status === 'DRY_RUN_FAIL',
    grey: status === 'FAILED',
    dryRun: status === 'DRY_RUN_PASS' || status === 'DRY_RUN_FAIL',
  });

  return (
    <StyledPill
      className={look}
      data-cy="ai-review-badge"
      data-cy-status={status}
      data-cy-last-failed={String(Boolean(lastFailed))}
    >
      {status === 'FAILED' ? <AlertCircle /> : <Stars01 />}
      {status !== 'FAILED' && score}
      {lastFailed && <AlertTriangle />}
    </StyledPill>
  );
};
