import { CurrentUsage } from '../../CurrentUsage/CurrentUsage';
import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { PlansCloudList } from './PlansCloudList';
import { useApiQuery, useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from '../../../useOrganization';
import { BillingPeriodType } from '../Price/PeriodSwitch';
import { useOrganizationCreditBalance } from '../../useOrganizationCreditBalance';
import { useEffect, useState } from 'react';
import { useReportEvent } from 'tg.hooks/useReportEvent';
import { StyledBillingSectionTitle } from '../../BillingSection';
import { planIsPeriodDependant } from '../Plan/plansTools';

const StyledShoppingGrid = styled('div')`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(300px, 100%), 1fr));
  gap: 16px;
  margin: 16px 0px;
`;

export const PlansCloud = () => {
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
            <Box display="flex" justifyContent="center">
              <StyledBillingSectionTitle>
                <T keyName="organization_cloud_plans_title" />
              </StyledBillingSectionTitle>
            </Box>
            <StyledShoppingGrid>
              <PlansCloudList
                plans={plansLoadable.data._embedded.plans}
                activeSubscription={activeSubscription.data}
                onPeriodChange={(period) => setPeriod(period)}
                period={period}
              />
            </StyledShoppingGrid>
          </>
        )}
    </>
  );
};