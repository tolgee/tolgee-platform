import { useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';

import { useOrganization } from '../../../useOrganization';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { PlanType } from '../cloud/Plans/types';
import { BillingPeriodType } from '../cloud/Plans/Price/PeriodSwitch';

export const StyledContainer = styled(Box)`
  justify-self: center;
  align-self: end;
  gap: 8px;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  white-space: nowrap;
`;

type Props = {
  plan: PlanType;
  period: BillingPeriodType;
};

export const SelfHostedPlanAction = ({ plan, period }: Props) => {
  const { t } = useTranslate();

  const organization = useOrganization();

  const subscribeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/self-hosted-ee/subscriptions',
    method: 'post',
    options: {
      onSuccess: (data) => {
        window.location.href = data.url;
      },
    },
  });

  const subscribeFreeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/self-hosted-ee/subscribe-free',
    method: 'post',
    options: {
      onSuccess: () => {},
    },
    invalidatePrefix: '/v2/organizations/{organizationId}/billing',
  });

  const onSubscribe = () => {
    if (plan.free) {
      subscribeFreeMutation.mutate({
        path: { organizationId: organization!.id },
        content: {
          'application/json': {
            planId: plan.id,
          },
        },
      });
      return;
    }
    subscribeMutation.mutate({
      path: { organizationId: organization!.id },
      content: {
        'application/json': {
          planId: plan.id,
          period: period,
        },
      },
    });
  };

  return (
    <StyledContainer>
      <LoadingButton
        data-cy="billing-self-hosted-ee-plan-subscribe-button"
        variant="contained"
        color="primary"
        size="small"
        loading={subscribeMutation.isLoading}
        onClick={onSubscribe}
      >
        {t('billing_plan_subscribe')}
      </LoadingButton>
    </StyledContainer>
  );
};
