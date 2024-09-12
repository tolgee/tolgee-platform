import { FC } from 'react';
import { Box, styled, useTheme } from '@mui/material';
import { components } from 'tg.service/billingApiSchema.generated';

import { SelfHostedEeSubscriptionActions } from './SelfHostedEeSubscriptionActions';
import {
  PlanContainer,
  PlanContent,
} from 'tg.component/billing/Plan/PlanStyles';
import { SelfHostedEeEstimatedCosts } from 'tg.component/billing/ActiveSubscription/SelfHostedEeEstimatedCosts';
import { ActivePlanTitle } from 'tg.component/billing/ActiveSubscription/ActivePlanTitle';
import { PricePrimary } from 'tg.component/billing/Price/PricePrimary';
import { PayAsYouGoPrices } from 'tg.component/billing/Price/PayAsYouGoPrices';
import { IncludedUsage } from 'tg.component/billing/Plan/IncludedUsage';
import { isPlanLegacy } from 'tg.component/billing/Plan/plansTools';
import { ActiveSubscriptionBanner } from 'tg.component/billing/ActiveSubscription/ActiveSubscriptionBanner';
import { CollapsedFeatures } from 'tg.component/billing/ActiveSubscription/CollapsedFeatures';

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
            status={subscription.status}
            createdAt={subscription.createdAt}
            periodStart={subscription.currentPeriodStart}
            periodEnd={subscription.currentPeriodEnd}
            highlightColor={highlightColor}
          />

          <SelfHostedEeEstimatedCosts subscription={subscription} />
        </Box>

        <CollapsedFeatures features={plan.enabledFeatures} custom={custom} />

        <Box
          display="flex"
          justifyContent="space-between"
          flexWrap="wrap"
          alignItems="center"
          mt={1}
          mb={1}
        >
          <IncludedUsage
            includedUsage={plan.includedUsage}
            isLegacy={isPlanLegacy(plan)}
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
