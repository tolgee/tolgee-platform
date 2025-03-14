import { Box, SxProps, Theme } from '@mui/material';
import { PlanType } from './types';
import {
  IncludedCredits,
  IncludedKeys,
  IncludedSeats,
  IncludedStrings,
  IncludedStringSlots,
} from '../IncludedItem';

type Props = {
  includedUsage: PlanType['includedUsage'];
  isLegacy: boolean;
  highlightColor: string;
  sx?: SxProps<Theme>;
  className?: string;
  metricType: PlanType['metricType'];
};

export const IncludedUsage = ({
  includedUsage,
  isLegacy,
  metricType,
  highlightColor,
  sx,
  className,
}: Props) => {
  return (
    <Box
      display="flex"
      flexDirection="column"
      justifySelf="center"
      {...{ sx, className }}
    >
      {isLegacy ? (
        <IncludedStringSlots
          data-cy={'billing-plan-included-translation-slots'}
          className="strings"
          count={includedUsage?.translationSlots ?? -1}
          highlightColor={highlightColor}
        />
      ) : (
        <>
          {metricType == 'STRINGS' && (
            <IncludedStrings
              data-cy={'billing-plan-included-strings'}
              className="strings"
              count={includedUsage?.translations ?? -1}
              highlightColor={highlightColor}
            />
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
      )}

      <IncludedCredits
        data-cy={'billing-plan-included-credits'}
        className="mt-credits"
        count={includedUsage?.mtCredits ?? -1}
        highlightColor={highlightColor}
      />
    </Box>
  );
};
