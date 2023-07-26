import { CurrentUsage } from '../../CurrentUsage/CurrentUsage';
import { Box, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { BillingPlans } from './Plans/BillingPlans';
import { Credits } from './Plans/Credits/Credits';
import { useApiQuery, useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useOrganization } from '../../../useOrganization';
import { BillingPeriodType } from './Plans/PeriodSwitch';
import { useOrganizationCreditBalance } from '../../useOrganizationCreditBalance';
import { useEffect, useState } from 'react';
import { planIsPeriodDependant } from './Plans/PlanPrice';
import { useReportEvent } from 'tg.hooks/useReportEvent';

const StyledShopping = styled('div')`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(450px, 100%), 1fr));
  gap: 16px;
  margin: 16px 0px;
`;

export const CloudSubscriptions = () => {
  const organization = useOrganization();

  const [period, setPeriod] = useState<BillingPeriodType>();
  const creditBalance = useOrganizationCreditBalance();

  const usage = useApiQuery({
    url: '/v2/organizations/{organizationId}/usage',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const plansLoadable = useBillingApiQuery({
    url: `/v2/organizations/{organizationId}/billing/plans`,
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const activeSubscription = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/subscription',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
    options: {
      onSuccess(data) {
        if (!period)
          if (data.plan && planIsPeriodDependant(data.plan.prices)) {
            setPeriod(data.currentBillingPeriod);
          } else {
            setPeriod('YEARLY');
          }
      },
    },
  });

  useGlobalLoading(activeSubscription.isLoading || plansLoadable.isLoading);

  const reportEvent = useReportEvent();

  useEffect(() => {
    reportEvent('BILLING_CLOUD_SUBSCRIPTIONS_VIEW');
  }, []);

  return (
    <>
      {plansLoadable.data?._embedded?.plans &&
        activeSubscription.data &&
        usage.data &&
        creditBalance.data &&
        period && (
          <>
            <Box mb={2}>
              <CurrentUsage
                activeSubscription={activeSubscription.data}
                usage={usage.data}
                balance={creditBalance.data}
              />
            </Box>
            <Typography variant="h6" mt={2}>
              <T keyName="organization_pricing_plans_title" />
            </Typography>
            <StyledShopping>
              <BillingPlans
                plans={plansLoadable.data._embedded.plans}
                activeSubscription={activeSubscription.data}
                onPeriodChange={(period) => setPeriod(period)}
                period={period}
              />
              <Credits />
            </StyledShopping>
          </>
        )}
    </>
  );
};
