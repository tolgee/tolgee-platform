import { styled, useTheme } from '@mui/material';
import React from 'react';

import { Stars } from 'tg.component/CustomIcons';
import { getAiReviewOutcomeColors } from 'tg.ee.module/aiReview/utils/scoreColor';
import { AiReviewOutcome } from 'tg.ee.module/aiReview/utils/types';

export const StyledAiReviewPill = styled('span')`
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 20px;
  padding: 0px 7px 0px 5px;
  border-radius: 10px;
  border: 1px solid transparent;
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;

  & svg {
    width: 13px;
    height: 13px;
    flex-shrink: 0;
  }
`;

type Props = React.ComponentProps<typeof StyledAiReviewPill> & {
  outcome: AiReviewOutcome | undefined;
  score: number | undefined;
  dryRun?: boolean;
};

export const AiReviewScoreChip = React.forwardRef<HTMLSpanElement, Props>(
  function AiReviewScoreChip({ outcome, score, dryRun, style, ...props }, ref) {
    const theme = useTheme();
    const colors = getAiReviewOutcomeColors(
      theme,
      outcome ?? 'PENDING',
      dryRun
    );
    return (
      <StyledAiReviewPill
        ref={ref}
        style={{
          background: colors.background,
          color: colors.color,
          borderColor: colors.border,
          ...style,
        }}
        {...props}
      >
        <Stars />
        {score == null ? '!' : score}
      </StyledAiReviewPill>
    );
  }
);
