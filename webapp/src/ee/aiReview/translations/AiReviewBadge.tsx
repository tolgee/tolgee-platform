import clsx from 'clsx';
import { alpha, styled } from '@mui/material';
import {
  AlertCircle,
  AlertTriangle,
  DotsHorizontal,
  Stars01,
  XClose,
} from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { isScoreOnlyStatus } from 'tg.ee.module/aiReview/translations/aiReviewVisibility';

type AiReviewSummary = components['schemas']['TranslationAiReviewSummaryModel'];

const StyledPill = styled('span')`
  display: inline-flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.25)};
  border-radius: 10px;
  padding: ${({ theme }) => theme.spacing(0.375, 0.75)};
  font-size: ${({ theme }) => theme.typography.overline.fontSize}px;
  font-weight: ${({ theme }) => theme.typography.fontWeightMedium};
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
  &.scoreOnlyRun {
    background: transparent;
    border: 1px solid;
  }
  &.queued {
    opacity: 0.6;
  }
`;

type Props = {
  aiReview: AiReviewSummary;
};

export const AiReviewBadge = ({ aiReview }: Props) => {
  const { status, score, lastFailed, queued } = aiReview;

  const scoreOnlyRun = isScoreOnlyStatus(status);
  const look = clsx({
    green: status === 'APPROVED',
    orange: status === 'BELOW_THRESHOLD',
    grey:
      status === 'FAILED' ||
      status === 'OVERRIDDEN' ||
      status === 'PENDING' ||
      scoreOnlyRun,
    scoreOnlyRun,
    queued,
  });

  return (
    <StyledPill
      className={look}
      data-cy="ai-review-badge"
      data-cy-status={status}
      data-cy-last-failed={String(Boolean(lastFailed))}
      data-cy-queued={String(Boolean(queued))}
    >
      {status === 'FAILED' ? <AlertCircle /> : <Stars01 />}
      {status === 'OVERRIDDEN' && <XClose />}
      {status !== 'FAILED' && status !== 'OVERRIDDEN' && score}
      {queued && <DotsHorizontal />}
      {lastFailed && <AlertTriangle />}
    </StyledPill>
  );
};
