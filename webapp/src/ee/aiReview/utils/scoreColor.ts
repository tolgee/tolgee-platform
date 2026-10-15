import { Theme } from '@mui/material';
import { AiReviewOutcome } from 'tg.ee.module/aiReview/utils/types';

export type ScoreColors = {
  background: string;
  color: string;
  border: string;
};

export const getAiReviewOutcomeColors = (
  theme: Theme,
  outcome: AiReviewOutcome,
  dryRun = false
): ScoreColors => {
  const { tokens } = theme.palette;
  if (dryRun && outcome === 'BELOW_THRESHOLD') {
    return {
      background: 'transparent',
      color: tokens.warning.dark,
      border: tokens.warning._states.outlinedBorder,
    };
  }
  switch (outcome) {
    case 'AUTO_REVIEWED':
    case 'CONFIRMED':
      return {
        background: tokens._components.alert.success.background,
        color: tokens._components.alert.success.color,
        border: tokens._components.alert.success.background,
      };
    case 'WOULD_AUTO_REVIEW':
      return {
        background: 'transparent',
        color: tokens.success.dark,
        border: tokens.success._states.outlinedBorder,
      };
    case 'BELOW_THRESHOLD':
      return {
        background: tokens._components.alert.warning.background,
        color: tokens._components.alert.warning.color,
        border: tokens._components.alert.warning.background,
      };
    default:
      return {
        background: theme.palette.emphasis[100],
        color: theme.palette.text.secondary,
        border: theme.palette.emphasis[100],
      };
  }
};
