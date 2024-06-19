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
import { ShowAllFeaturesLink } from './ShowAllFeaturesLink';
import { PlanType } from './types';
import { IncludedUsage } from './IncludedUsage';
import { ContactUsButton } from './ContactUsButton';
import { isPlanLegacy } from './plansTools';
import { Box } from '@mui/material';

type Features = PlanType['enabledFeatures'];

type Props = {
  plan: PlanType;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
  isActive: boolean;
  isEnded: boolean;
  filteredFeatures: Features;
  topFeature?: React.ReactNode;
  featuresMinHeight?: string;
  action: React.ReactNode;
};

export const Plan: FC<Props> = ({
  plan,
  period,
  onPeriodChange,
  isActive,
  isEnded,
  filteredFeatures,
  topFeature,
  featuresMinHeight,
  action,
}) => {
  return (
    <PlanContainer
      className={clsx({ active: isActive })}
      data-cy="billing-plan"
    >
      <PlanActiveBanner isActive={isActive} isEnded={isEnded} />
      <PlanContent>
        <PlanTitle sx={{ paddingBottom: '20px' }}>{plan.name}</PlanTitle>

        <PlanFeaturesBox sx={{ gap: '18px' }}>
          <IncludedFeatures
            sx={{ minHeight: featuresMinHeight }}
            features={filteredFeatures}
            topFeature={topFeature}
          />
          {plan.public && <ShowAllFeaturesLink />}
          <IncludedUsage
            includedUsage={plan.includedUsage}
            isLegacy={isPlanLegacy(plan)}
          />
        </PlanFeaturesBox>

        {plan.prices && (
          <PlanPrice
            sx={{ paddingTop: '20px' }}
            prices={plan.prices}
            period={period}
            onPeriodChange={onPeriodChange}
          />
        )}

        <Box sx={{ paddingTop: '20px', justifySelf: 'center' }}>
          {plan.type === 'CONTACT_US' ? <ContactUsButton /> : action}
        </Box>
      </PlanContent>
    </PlanContainer>
  );
};
