import { Box, styled, SxProps, Theme } from '@mui/material';
import { Stars01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { PlanType } from './types';
import {
  IncludedCredits,
  IncludedKeys,
  IncludedSeats,
  IncludedStrings,
  IncludedWords,
} from '../IncludedItem';

/**
 * A temporary bonus rather than a permanent allowance, so it is set apart from the included items
 * instead of listed among them. Teal-on-teal is the same token pair PlanSubtitle uses.
 */
const StyledBoost = styled(Box)`
  display: flex;
  align-items: center;
  align-self: center;
  gap: 6px;
  margin-top: 10px;
  padding: 6px 14px;
  border-radius: 999px;
  font-weight: 600;
  line-height: 1.25;
  color: ${({ theme }) => theme.palette.tokens.secondary.main};
  background: ${({ theme }) => theme.palette.tokens.secondary._states.selected};
`;

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
        <StyledBoost data-cy="billing-plan-onboarding-boost">
          <Stars01 width={18} height={18} />
          <span>
            <T
              keyName="billing_plan_onboarding_boost"
              defaultValue="+{credits, number} MT credits for your first {months, plural, one {month} other {# months}}"
              params={{
                credits: onboardingBoostCredits!,
                months: onboardingBoostMonths!,
              }}
            />
          </span>
        </StyledBoost>
      )}
    </Box>
  );
};
