import { FC } from 'react';
import clsx from 'clsx';

import { BillingPeriodType } from '../Price/PeriodSwitch';
import {
  PlanFeaturesBox,
  PlanContainer,
  PlanContent,
  PlanTitle,
} from './PlanStyles';
import { PlanPrice } from '../Price/PlanPrice';
import { IncludedFeatures } from './IncludedFeatures';
import { PlanActiveBanner } from './PlanActiveBanner';
import { ShowAllFeaturesLink } from './ShowAllFeatures';
import { PlanType } from './types';
import { IncludedUsage } from './IncludedUsage';
import { ContactUsButton } from './ContactUsButton';
import { isPlanLegacy } from './plansTools';
import { Box, useTheme } from '@mui/material';

type Features = PlanType['enabledFeatures'];

type Props = {
  plan: PlanType;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
  active: boolean;
  ended: boolean;
  filteredFeatures: Features;
  topFeature?: React.ReactNode;
  featuresMinHeight?: string;
  action: React.ReactNode;
  custom?: boolean;
};

export const Plan: FC<Props> = ({
  plan,
  period,
  onPeriodChange,
  active,
  ended,
  filteredFeatures,
  topFeature,
  featuresMinHeight,
  action,
  custom,
}) => {
  const theme = useTheme();
  const highlightColor = custom
    ? theme.palette.tokens.info.main
    : theme.palette.tokens.primary.main;

  return (
    <PlanContainer className={clsx({ active })} data-cy="billing-plan">
      <PlanActiveBanner active={active} ended={ended} custom={custom} />
      <PlanContent>
        <PlanTitle sx={{ paddingBottom: '20px', color: highlightColor }}>
          {plan.name}
        </PlanTitle>

        <Box
          display="flex"
          flexDirection="column"
          alignItems="stretch"
          flexGrow={1}
        >
          <PlanFeaturesBox sx={{ gap: '18px' }}>
            <IncludedFeatures
              sx={{ minHeight: featuresMinHeight }}
              features={filteredFeatures}
              topFeature={topFeature}
            />
            {plan.public && (
              <ShowAllFeaturesLink sx={{ alignSelf: 'center' }} />
            )}
            <IncludedUsage
              includedUsage={plan.includedUsage}
              isLegacy={isPlanLegacy(plan)}
              highlightColor={highlightColor}
              sx={{ alignSelf: 'center' }}
            />
          </PlanFeaturesBox>
        </Box>

        {plan.prices && (
          <PlanPrice
            sx={{ paddingTop: '20px' }}
            prices={plan.prices}
            period={period}
            onPeriodChange={onPeriodChange}
            highlightColor={highlightColor}
          />
        )}

        <Box sx={{ paddingTop: '20px', alignSelf: 'center' }}>
          {plan.type === 'CONTACT_US' ? <ContactUsButton /> : action}
        </Box>
      </PlanContent>
    </PlanContainer>
  );
};
