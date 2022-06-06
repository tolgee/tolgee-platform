import { FC } from 'react';
import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/billingApiSchema.generated';
import { PlanInfo } from './PlanInfo';
import { usePlan } from './usePlan';
import { PlanActionButton } from './PlanActionButton';
import { PlanTitle } from './PlanTitle';
import { PlanPrice } from './PlanPrice';

type PlanModel = components['schemas']['PlanModel'];
type Period = components['schemas']['SubscribeRequest']['period'];

const StyledPlan = styled('div')`
  background: ${({ theme }) => theme.palette.emphasis[200]};
  border: 1px solid ${({ theme }) => theme.palette.emphasis[200]};
  border-radius: 20px;
  padding: 20px;
  display: grid;
  gap: 8px;
  grid-template-areas:
    'title    title'
    'info     info'
    'price    action';
`;

type Props = {
  plan: PlanModel;
  isOrganizationSubscribed: boolean;
  period: Period;
  isActive: boolean;
};

export const Plan: FC<Props> = ({
  plan,
  isOrganizationSubscribed,
  period,
  isActive,
}) => {
  const t = useTranslate();
  const { onUpgrade, upgradeMutation, onSubscribe, subscribeMutation } =
    usePlan({ planId: plan.id, period: period });

  return (
    <StyledPlan>
      <PlanTitle title={plan.name} />
      <PlanInfo plan={plan} />
      <PlanPrice
        price={period === 'MONTHLY' ? plan.monthlyPrice : plan.yearlyPrice}
        period={period}
      />

      {!plan.free &&
        (isActive ? null : isOrganizationSubscribed ? (
          <PlanActionButton
            loading={upgradeMutation.isLoading}
            onClick={() => onUpgrade()}
          >
            {t('billing_plan_subscribe')}
          </PlanActionButton>
        ) : (
          <PlanActionButton
            loading={subscribeMutation.isLoading}
            onClick={() => onSubscribe()}
          >
            {t('billing_plan_subscribe')}
          </PlanActionButton>
        ))}
    </StyledPlan>
  );
};
