import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { CancelSelfHostedEeSubscriptionButton } from '../../CancelSelfHostedEeSubscriptionButton';
import { IncludedFeatures } from './IncludedFeatures';
import { Plan, PlanContent } from '../common/Plan';
import { PlanPrice } from '../cloud/Plans/PlanPrice';
import { PlanInfoArea } from '../common/PlanInfo';
import { SelfHostedEeEstimatedCosts } from './SelfHostedEeEstimatedCosts';
import { ActivePlanTitle } from './ActivePlanTitle';

export const SelfHostedEeActiveSubscription: FC<{
  subscription: components['schemas']['SelfHostedEeSubscriptionModel'];
}> = ({ subscription }) => {
  return (
    <Plan>
      <PlanContent>
        <ActivePlanTitle
          name={subscription.plan.name}
          status={subscription.status}
          createdAt={subscription.createdAt}
          licenseKey={subscription.licenseKey}
        />

        <SelfHostedEeEstimatedCosts subscription={subscription} />

        <PlanInfoArea>
          <IncludedFeatures features={subscription.plan.enabledFeatures} />
        </PlanInfoArea>

        <PlanPrice
          pricePerSeat={subscription.plan.pricePerSeat}
          subscriptionPrice={subscription.plan.subscriptionPrice}
        />
        <CancelSelfHostedEeSubscriptionButton id={subscription.id} />
      </PlanContent>
    </Plan>
  );
};
