import { useEffect, useState } from 'react';
import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { useApiQuery, useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';

import { useReportEvent } from 'tg.hooks/useReportEvent';

import { StyledBillingSectionTitle } from '../../BillingSection';
import { useOrganizationCreditBalance } from '../../useOrganizationCreditBalance';
import { CurrentCloudSubscriptionInfo } from '../../currentCloudSubscription/CurrentCloudSubscriptionInfo';
import { PlansCloudList } from './PlansCloudList';
import { useLocation } from 'react-router-dom';
import { BillingPeriodType } from '../../component/Price/PeriodSwitch';
import { isPlanPeriodDependant } from '../../component/Plan/plansTools';
import { components } from 'tg.service/billingApiSchema.generated';

const StyledShoppingGrid = styled('div')`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
  gap: 16px;
  margin: 16px 0px;
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

  function setInitialPeriod(
    data: components['schemas']['CloudSubscriptionModel']
  ) {
    if (isPlanPeriodDependant(data.plan.prices) && data.currentBillingPeriod) {
      setPeriod(data.currentBillingPeriod);
      return;
    }
    setPeriod('YEARLY');
  }

  const activeSubscription = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/subscription',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
    options: {
      onSuccess(data) {
        setInitialPeriod(data);
      },
    },
  });

  const reportEvent = useReportEvent();

  useEffect(() => {
    reportEvent('BILLING_CLOUD_SUBSCRIPTIONS_VIEW');
  }, []);

  const { search } = useLocation();

  const params = new URLSearchParams(search);

  const isSuccess = params.get('success') === '';

  useEffect(() => {
    if (isSuccess) {
      reportEvent('UI_CLOUD_SUBSCRIPTION_UPDATE_SUCCESS');
    }
  }, [isSuccess]);

  return (
    <>
      {activeSubscription.data && usage.data && creditBalance.data && (
        <>
          <Box>
            <CurrentCloudSubscriptionInfo
              activeSubscription={activeSubscription.data}
              usage={usage.data}
            />
          </Box>
          <Box display="flex" justifyContent="center">
            <StyledBillingSectionTitle>
              <T keyName="organization_cloud_plans_title" />
            </StyledBillingSectionTitle>
          </Box>
          <StyledShoppingGrid>
            <PlansCloudList
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
