import { FunctionComponent, useEffect, useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';
import { styled, Typography } from '@mui/material';

import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { LINKS } from 'tg.constants/links';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import {
  useApiQuery,
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { useOrganizationCreditBalance } from './useOrganizationCreditBalance';
import { BillingPlans } from './BillingPlans/BillingPlans';
import { Credits } from './BillingPlans/Credits/Credits';
import { CustomerPortal } from './CustomerPortal/CustomerPortal';
import { CurrentUsage } from './CurrentUsage/CurrentUsage';
import { BillingPeriodType } from './BillingPlans/PeriodSwitch';
import { Invoices } from './Invoices/Invoices';
import { useMessage } from 'tg.hooks/useSuccessMessage';

const StyledCurrent = styled('div')`
  display: grid;
  grid-template-areas:
    'usage customerPortal'
    'usage invoices';
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

export const OrganizationBillingView: FunctionComponent = () => {
  const { search, pathname } = useLocation();
  const params = new URLSearchParams(search);
  const history = useHistory();

  const success = params.has('success');
  const mtCreditsSuccess = params.has('buy-mt-credits-success');

  const messaging = useMessage();

  const organization = useOrganization();

  const creditBalance = useOrganizationCreditBalance();
  const { t } = useTranslate();

  const url = new URL(window.location.href);

  const [period, setPeriod] = useState<BillingPeriodType>('YEARLY');

  url.search = '';

  const refreshSubscription = useBillingApiMutation({
    url: `/v2/organizations/{organizationId}/billing/refresh-subscription`,
    method: `put`,
    invalidatePrefix: `/`,
  });

  useEffect(() => {
    if (success) {
      refreshSubscription.mutate({
        path: { organizationId: organization!.id },
      });
      messaging.success(<T keyName="billing_plan_update_success_message" />);
      history.replace(pathname);
    }
  }, [success]);

  useEffect(() => {
    if (mtCreditsSuccess) {
      messaging.success(
        <T keyName="billing_mt_credit_purchase_success_message" />
      );
      history.replace(pathname);
    }
  }, [mtCreditsSuccess]);

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

  const usage = useApiQuery({
    url: '/v2/organizations/{organizationId}/usage',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  return (
    <BaseOrganizationSettingsView
      title={t('organization_billing_title')}
      hideChildrenOnLoading={true}
      loading={
        activePlan.isLoading ||
        plansLoadable.isLoading ||
        refreshSubscription.isLoading ||
        activePlan.isLoading ||
        usage.isLoading
      }
      link={LINKS.ORGANIZATION_BILLING}
      navigation={[
        [
          t('organization_menu_billing'),
          LINKS.ORGANIZATION_BILLING.build({ slug: organization!.slug }),
        ],
      ]}
      windowTitle={t({ key: 'organization_billing_title', noWrap: true })}
      containerMaxWidth="xl"
    >
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
              <Invoices />
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
    </BaseOrganizationSettingsView>
  );
};
