import { CurrentUsage } from '../../CurrentUsage/CurrentUsage';
import { CustomerPortal } from '../../CustomerPortal/CustomerPortal';
import { styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { BillingPlans } from './Plans/BillingPlans';
import { Credits } from './Plans/Credits/Credits';
import { useApiQuery, useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useOrganization } from '../../../useOrganization';
import { useState } from 'react';
import { BillingPeriodType } from './Plans/PeriodSwitch';
import { useOrganizationCreditBalance } from '../../useOrganizationCreditBalance';

const StyledCurrent = styled('div')`
  display: grid;
  grid-template-areas: 'usage customerPortal';
  grid-template-columns: 1fr 1fr;
  align-items: start;
  justify-content: space-between;
  gap: 32px 24px;
  margin-bottom: 32px;
  @media (max-width: 1400px) {
    grid-template-columns: 1fr;
    grid-template-areas:
      'usage'
      'customerPortal'
      'invoices';
  }
`;

const StyledShopping = styled('div')`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
  gap: 16px;
  margin: 16px 0px;
  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
`;

export const CloudSubscriptions = () => {
  const organization = useOrganization();

  const [period, setPeriod] = useState<BillingPeriodType>('YEARLY');
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

  const activePlan = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/active-plan',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
    options: {
      onSuccess(data) {
        if (data.currentBillingPeriod) {
          setPeriod(data.currentBillingPeriod);
        }
      },
    },
  });

  useGlobalLoading(activePlan.isLoading || plansLoadable.isLoading);

  return (
    <>
      {plansLoadable.data?._embedded?.plans &&
        activePlan.data &&
        usage.data &&
        creditBalance.data && (
          <>
            <StyledCurrent>
              <CurrentUsage
                activePlan={activePlan.data}
                usage={usage.data}
                balance={creditBalance.data}
              />
              <CustomerPortal />
            </StyledCurrent>
            <Typography variant="h6">
              <T>organization_pricing_plans_title</T>
            </Typography>
            <StyledShopping>
              <BillingPlans
                plans={plansLoadable.data._embedded.plans}
                activePlan={activePlan.data}
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
