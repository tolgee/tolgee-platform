import { Box, SxProps, Theme, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { PlanType } from './types';
import {
  IncludedCredits,
  IncludedKeys,
  IncludedSeats,
  IncludedStrings,
  IncludedWords,
} from '../IncludedItem';

type Props = {
  includedUsage: PlanType['includedUsage'];
  highlightColor: string;
  sx?: SxProps<Theme>;
  className?: string;
  metricType: PlanType['metricType'];
  onboardingBoostMonths?: number;
  onboardingBoostCredits?: number;
};

export const IncludedUsage = ({
  includedUsage,
  metricType,
  highlightColor,
  sx,
  className,
  onboardingBoostMonths,
  onboardingBoostCredits,
}: Props) => {
  // Both halves are needed for the sentence to say anything: a boost of no credits, or credits for
  // no months, is not a boost.
  const hasBoost = Boolean(onboardingBoostMonths && onboardingBoostCredits);
  return (
    <Box
      display="flex"
      flexDirection="column"
      justifySelf="center"
      {...{ sx, className }}
    >
      <>
        {metricType == 'STRINGS' && (
          <IncludedStrings
            data-cy={'billing-plan-included-strings'}
            className="strings"
            count={includedUsage?.translations ?? -1}
            highlightColor={highlightColor}
          />
        )}

        {metricType == 'HOSTED_WORDS' && (
          <>
            <IncludedWords
              data-cy={'billing-plan-included-words'}
              className="words"
              count={includedUsage?.words ?? -1}
              highlightColor={highlightColor}
            />
            <IncludedSeats
              data-cy={'billing-plan-included-seats'}
              className="seats"
              count={includedUsage?.seats ?? -1}
              highlightColor={highlightColor}
            />
          </>
        )}

        {metricType == 'KEYS_SEATS' && (
          <>
            <IncludedKeys
              data-cy={'billing-plan-included-keys'}
              className="strings"
              count={includedUsage?.keys ?? -1}
              highlightColor={highlightColor}
            />
            <IncludedSeats
              data-cy={'billing-plan-included-seats'}
              className="seats"
              count={includedUsage?.seats ?? -1}
              highlightColor={highlightColor}
            />
          </>
        )}
      </>

      <IncludedCredits
        data-cy={'billing-plan-included-credits'}
        className="mt-credits"
        count={includedUsage?.mtCredits ?? -1}
        highlightColor={highlightColor}
      />

      {hasBoost && (
        <Typography
          variant="caption"
          color="text.secondary"
          data-cy="billing-plan-onboarding-boost"
        >
          <T
            keyName="billing_plan_onboarding_boost"
            defaultValue="+{credits, number} AI credits for your first {months, plural, one {month} other {# months}}"
            params={{
              credits: onboardingBoostCredits!,
              months: onboardingBoostMonths!,
            }}
          />
        </Typography>
      )}
    </Box>
  );
};
