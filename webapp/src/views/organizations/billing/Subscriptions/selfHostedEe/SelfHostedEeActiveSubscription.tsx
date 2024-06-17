import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';
import { components } from 'tg.service/billingApiSchema.generated';

import { SelfHostedEeSubscriptionActions } from '../../SelfHostedEeSubscriptionActions';
import { IncludedFeatures } from '../common/IncludedFeatures';
import {
  Plan,
  PlanContent,
  PlanFeaturesBox,
  PlanSubtitle,
} from '../common/Plan';
import { SelfHostedEeEstimatedCosts } from './SelfHostedEeEstimatedCosts';
import { ActivePlanTitle } from './ActivePlanTitle';
import { PlanDescription } from './PlanDescription';
import { PricePrimary } from '../cloud/Plans/Price/PricePrimary';
import { PayAsYouGoPrices } from '../cloud/Plans/Price/PayAsYouGoPrices';

type SelfHostedEeSubscriptionModel =
  components['schemas']['SelfHostedEeSubscriptionModel'];

const StyledPlanContent = styled(PlanContent)`
  display: grid;
  grid-template-rows: unset;
  align-items: center;
`;

const StyledFeatures = styled(IncludedFeatures)`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(200px, 100%), 1fr));
  margin: 0px;
`;

type Props = {
  subscription: SelfHostedEeSubscriptionModel;
  isNew: boolean;
};

export const SelfHostedEeActiveSubscription: FC<Props> = ({
  subscription,
  isNew,
}) => {
  const period = subscription.currentBillingPeriod;
  const { t } = useTranslate();

  const hasPrice = Boolean(
    subscription.plan.prices.subscriptionMonthly ||
      subscription.plan.prices.subscriptionYearly
  );

  return (
    <Plan className="active" data-cy="self-hosted-ee-active-plan">
      <StyledPlanContent>
        {isNew && <PlanSubtitle>{t('billing_subscription_new')}</PlanSubtitle>}
        <Box display="flex" justifyContent="space-between">
          <ActivePlanTitle
            name={subscription.plan.name}
            status={subscription.status}
            createdAt={subscription.createdAt}
            periodStart={subscription.currentPeriodStart}
            periodEnd={subscription.currentPeriodEnd}
          />

          <SelfHostedEeEstimatedCosts subscription={subscription} />
        </Box>

        <Box>
          <PlanDescription free={subscription.plan.free} hasPrice={hasPrice} />
        </Box>
        <PlanFeaturesBox sx={{ gap: '18px' }}>
          <StyledFeatures features={subscription.plan.enabledFeatures} />
        </PlanFeaturesBox>
        <PayAsYouGoPrices
          sx={{ justifySelf: 'start', mt: 2 }}
          hideTitle
          prices={subscription.plan.prices}
        />
        <Box
          display="flex"
          justifyContent="space-between"
          alignItems="center"
          mt={1}
        >
          <PricePrimary prices={subscription.plan.prices} period={period} />
          <SelfHostedEeSubscriptionActions
            id={subscription.id}
            licenceKey={subscription.licenseKey}
            isNew={isNew}
          />
        </Box>
      </StyledPlanContent>
    </Plan>
  );
};
