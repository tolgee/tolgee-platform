import { Box, styled, SxProps, Theme } from '@mui/material';
import { T } from '@tolgee/react';
import { SecondaryChip } from 'tg.component/common/chips/SecondaryChip';
import { PlanType } from './types';
import {
  IncludedCredits,
  IncludedKeys,
  IncludedSeats,
  IncludedStrings,
  IncludedWords,
} from '../IncludedItem';
import { Stars } from 'tg.component/CustomIcons';

const StyledBoostChip = styled(SecondaryChip)`
  align-self: center;
  margin-top: ${({ theme }) => theme.spacing(1)};
  max-width: 100%;
  height: auto;

  & .MuiChip-label {
    white-space: normal;
    text-align: center;
    padding: ${({ theme }) => theme.spacing(0.5)} 0;
    font-size: ${({ theme }) => theme.typography.caption.fontSize}px;
  }
`;

type Props = {
  includedUsage: PlanType['includedUsage'];
  highlightColor: string;
  sx?: SxProps<Theme>;
  className?: string;
  metricType: PlanType['metricType'];
  free?: PlanType['free'];
  onboardingBoostMonths?: number;
  onboardingBoostCredits?: number;
};

export const IncludedUsage = ({
  includedUsage,
  metricType,
  free,
  highlightColor,
  sx,
  className,
  onboardingBoostMonths,
  onboardingBoostCredits,
}: Props) => {
  const hasBoost = Boolean(onboardingBoostMonths && onboardingBoostCredits);
  const wordPlanSeats = free ? includedUsage?.seats ?? -1 : -1;
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
              count={wordPlanSeats}
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
        <StyledBoostChip
          data-cy="billing-plan-onboarding-boost"
          size="small"
          icon={<Stars width={16} height={16} />}
          label={
            <T
              keyName="billing_plan_onboarding_boost"
              defaultValue="+{credits, number} MT credits for your first {months, plural, one {month} other {# months}}"
              params={{
                credits: onboardingBoostCredits!,
                months: onboardingBoostMonths!,
              }}
            />
          }
        />
      )}
    </Box>
  );
};
