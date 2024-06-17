import { FC } from 'react';
import clsx from 'clsx';

import { usePlan } from './usePlan';
import { BillingPeriodType } from './Price/PeriodSwitch';
import {
  PlanFeaturesBox,
  Plan,
  PlanContent,
  PlanTitle,
} from '../../common/Plan';
import { PlanPrice } from './Price/PlanPrice';
import { IncludedFeatures } from '../../common/IncludedFeatures';
import { PlanActiveBanner } from '../../common/PlanActiveBanner';
import { PlanAction } from './PlanAction';
import { ShowAllFeaturesLink } from '../../common/ShowAllFeaturesLink';
import { PlanType } from './types';
import { IncludedUsage } from './IncludedUsage';
import { ContactUsButton } from './ContactUsButton';
import { isPlanLegacy } from './utils';

type Features = PlanType['enabledFeatures'];

type Props = {
  plan: PlanType;
  organizationHasSomeSubscription: boolean;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
  isActive: boolean;
  isEnded: boolean;
  filteredFeatures: Features;
  topFeature?: React.ReactNode;
  featuresMinHeight?: string;
};

export const CloudPlan: FC<Props> = ({
  plan,
  organizationHasSomeSubscription,
  period,
  onPeriodChange,
  isActive,
  isEnded,
  filteredFeatures,
  topFeature,
  featuresMinHeight,
}) => {
  const actions = usePlan({ planId: plan.id, period: period });

  return (
    <Plan className={clsx({ active: isActive })} data-cy="billing-plan">
      <PlanActiveBanner isActive={isActive} isEnded={isEnded} />
      <PlanContent>
        <PlanTitle sx={{ paddingBottom: '20px' }}>{plan.name}</PlanTitle>

        <PlanFeaturesBox sx={{ gap: '18px' }}>
          {Boolean(plan.enabledFeatures.length) && (
            <IncludedFeatures
              sx={{ minHeight: featuresMinHeight }}
              features={filteredFeatures}
              topFeature={topFeature}
            />
          )}
          {plan.public && <ShowAllFeaturesLink />}
          <IncludedUsage
            includedUsage={plan.includedUsage}
            isLegacy={isPlanLegacy(plan)}
          />
        </PlanFeaturesBox>

        {plan.prices && (
          <PlanPrice
            sx={{ paddingTop: '20px', paddingBottom: '20px' }}
            prices={plan.prices}
            period={period}
            onPeriodChange={onPeriodChange}
          />
        )}

        {plan.type === 'CONTACT_US' ? (
          <ContactUsButton />
        ) : (
          <PlanAction
            {...{
              actions,
              isActive,
              isEnded,
              organizationHasSomeSubscription,
              plan,
            }}
          />
        )}
      </PlanContent>
    </Plan>
  );
};
