import * as React from 'react';
import { FC } from 'react';
import {
  IncludedCredits,
  IncludedKeys,
  IncludedSeats,
  IncludedStrings,
} from '../../IncludedItem';
import { PlanType } from '../types';
import { useTheme } from '@mui/material';

export interface FreePlanLimitsProps {
  plan: PlanType;
}

export const FreePlanLimits: FC<FreePlanLimitsProps> = ({ plan }) => {
  const theme = useTheme();
  const highlightColor = theme.palette.primary.main;

  return (
    <>
      {plan.includedUsage && (
        <>
          {plan.metricType == 'STRINGS' && (
            <IncludedStrings
              sx={{ gridArea: 'metric1' }}
              count={plan.includedUsage.translations}
              highlightColor={highlightColor}
            />
          )}

          {plan.metricType == 'KEYS_SEATS' && (
            <IncludedKeys
              sx={{ gridArea: 'metric1' }}
              count={plan.includedUsage.keys}
              highlightColor={highlightColor}
            />
          )}

          <IncludedSeats
            sx={{ gridArea: 'metric2', justifySelf: 'center' }}
            count={plan.includedUsage.seats}
            highlightColor={highlightColor}
          />

          <IncludedCredits
            sx={{ gridArea: 'metric3', justifySelf: 'end' }}
            count={plan.includedUsage.mtCredits}
            highlightColor={highlightColor}
          />
        </>
      )}
    </>
  );
};
