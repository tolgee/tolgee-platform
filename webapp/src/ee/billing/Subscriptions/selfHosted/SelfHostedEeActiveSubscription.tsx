import { FC } from 'react';
import { Box, styled, useTheme } from '@mui/material';
import { components } from 'tg.service/billingApiSchema.generated';

import { SelfHostedEeSubscriptionActions } from './SelfHostedEeSubscriptionActions';
import { PlanContainer, PlanContent } from '../../component/Plan/PlanStyles';
import { ActiveSubscriptionBanner } from 'tg.ee.module/billing/component/ActiveSubscription/ActiveSubscriptionBanner';
import { ActivePlanTitle } from 'tg.ee.module/billing/component/ActiveSubscription/ActivePlanTitle';
import { SelfHostedEeEstimatedCosts } from 'tg.ee.module/billing/component/ActiveSubscription/SelfHostedEeEstimatedCosts';
import { CollapsedFeatures } from 'tg.ee.module/billing/component/ActiveSubscription/CollapsedFeatures';
import { IncludedUsage } from 'tg.ee.module/billing/component/Plan/IncludedUsage';
import { PayAsYouGoPrices } from 'tg.ee.module/billing/component/Price/PayAsYouGoPrices';
import { PricePrimary } from '../../component/Price/PricePrimary';
import { SelfHostedEeSubscriptionMetrics } from './SelfHostedEeSubscriptionMetrics';

type SelfHostedEeSubscriptionModel =
  components['schemas']['SelfHostedEeSubscriptionModel'];

const StyledPlanContent = styled(PlanContent)`
  display: grid;
  grid-template-rows: unset;
  align-items: center;
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

  const theme = useTheme();
  const plan = subscription.plan;

  const custom = !subscription.plan.public;

  const highlightColor = custom
    ? theme.palette.info.main
    : theme.palette.primary.main;

  return (
    <PlanContainer className="active" data-cy="self-hosted-ee-active-plan">
      <StyledPlanContent>
        <ActiveSubscriptionBanner
          custom={custom}
          status={subscription.status}
        />
        <Box display="flex" justifyContent="space-between">
          <ActivePlanTitle
            name={plan.name}
            nonCommercial={plan.nonCommercial}
            status={subscription.status}
            createdAt={subscription.createdAt}
            periodStart={subscription.currentPeriodStart}
            periodEnd={subscription.currentPeriodEnd}
            highlightColor={highlightColor}
          />

          <SelfHostedEeEstimatedCosts subscription={subscription} />
        </Box>

        <CollapsedFeatures features={plan.enabledFeatures} custom={custom} />
        <SelfHostedEeSubscriptionMetrics subscription={subscription} />
        <Box
          display="flex"
          justifyContent="space-between"
          flexWrap="wrap"
          alignItems="center"
          mt={1}
          mb={1}
        >
          <IncludedUsage
            metricType={'KEYS_SEATS'}
            includedUsage={plan.includedUsage}
            highlightColor={highlightColor}
          />
          <PayAsYouGoPrices
            sx={{ justifySelf: 'start' }}
            prices={plan.prices}
          />
        </Box>
        <Box
          display="flex"
          justifyContent="space-between"
          flexWrap="wrap"
          alignItems="center"
          mt={1}
        >
          <PricePrimary
            prices={plan.prices}
            period={period}
            highlightColor={highlightColor}
          />
          <SelfHostedEeSubscriptionActions
            id={subscription.id}
            licenceKey={subscription.licenseKey}
            isNew={isNew}
            custom={custom}
          />
        </Box>
      </StyledPlanContent>
    </PlanContainer>
  );
};
