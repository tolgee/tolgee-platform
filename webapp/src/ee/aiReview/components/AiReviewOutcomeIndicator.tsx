import { styled, useTheme } from '@mui/material';

import { Stars } from 'tg.component/CustomIcons';
import { StyledAiReviewPill } from 'tg.ee.module/aiReview/components/AiReviewScoreChip';
import { getAiReviewOutcomeColors } from 'tg.ee.module/aiReview/utils/scoreColor';
import { AiReviewFilterOutcome } from 'tg.views/projects/translations/TranslationFilters/tools';

const StyledIndicator = styled(StyledAiReviewPill)`
  justify-content: center;
  min-width: 26px;
  padding: 0px 5px;
  box-sizing: border-box;
`;

type Props = {
  outcome: AiReviewFilterOutcome;
};

export const AiReviewOutcomeIndicator = ({ outcome }: Props) => {
  const theme = useTheme();
  if (outcome === 'NOT_REVIEWED') {
    return null;
  }
  const colors =
    outcome === 'WOULD_NOT_AUTO_REVIEW'
      ? getAiReviewOutcomeColors(theme, 'BELOW_THRESHOLD', true)
      : getAiReviewOutcomeColors(theme, outcome);
  return (
    <StyledIndicator
      style={{
        background: colors.background,
        color: colors.color,
        borderColor: colors.border,
      }}
    >
      <Stars />
      {outcome === 'FAILED' && '!'}
    </StyledIndicator>
  );
};
