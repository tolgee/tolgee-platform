import React, { FC } from 'react';
import { SelfHostedEePlanForm } from './SelfHostedEePlanForm';
import { T } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useHistory } from 'react-router-dom';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { getSelfHostedPlanInitialValues } from 'tg.ee.module/billing/administration/subscriptionPlans/components/planForm/selfHostedEe/getSelfHostedPlanInitialValues';

type EditSelfHostedEePlanFormProps = { planId: number };

export const SelfHostedEePlanEditForm: FC<EditSelfHostedEePlanFormProps> = ({
  planId,
}) => {
  const messaging = useMessage();
  const history = useHistory();

  const planLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans/{planId}',
    method: 'get',
    path: { planId },
  });

  const planEditMutation = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/{planId}',
    method: 'put',
    invalidatePrefix: '/v2/administration/billing/self-hosted-ee-plans',
  });

  if (planLoadable.isLoading) {
    return <SpinnerProgress />;
  }

  const planData = getSelfHostedPlanInitialValues(planLoadable.data, true);

  const onSubmit: React.ComponentProps<
    typeof SelfHostedEePlanForm
  >['onSubmit'] = (values) => {
    planEditMutation.mutate(
      {
        path: { planId },
        content: {
          'application/json': {
            ...values,
            stripeProductId: values.stripeProductId!,
            forOrganizationIds: values.public ? [] : values.forOrganizationIds,
          },
        },
      },
      {
        onSuccess() {
          messaging.success(
            <T keyName="administration_ee_plan_updated_success" />
          );
          history.push(LINKS.ADMINISTRATION_BILLING_EE_PLANS.build());
        },
      }
    );
  };

  if (!planData) {
    return null;
  }

  return (
    <SelfHostedEePlanForm
      loading={false}
      initialData={planData}
      isUpdate={true}
      onSubmit={onSubmit}
      canEditPrices={planData.canEditPrices}
    />
  );
};
