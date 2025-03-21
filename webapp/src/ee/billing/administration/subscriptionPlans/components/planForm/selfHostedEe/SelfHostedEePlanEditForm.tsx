import React, { FC } from 'react';
import { SelfHostedEePlanForm } from './SelfHostedEePlanForm';
import { T } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useHistory } from 'react-router-dom';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';

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

  if (planLoadable.isLoading) {
    return <SpinnerProgress />;
  }

  const planData = planLoadable.data;

  if (!planData) {
    return null;
  }

  const planEditLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/{planId}',
    method: 'put',
    invalidatePrefix: '/v2/administration/billing/self-hosted-ee-plans',
  });

  return (
    <SelfHostedEePlanForm
      loading={false}
      initialData={{
        ...planData,
      }}
      isUpdate={true}
      onSubmit={(values) => {
        planEditLoadable.mutate(
          {
            path: { planId },
            content: {
              'application/json': {
                ...values,
                stripeProductId: values.stripeProductId!,
                forOrganizationIds: values.public
                  ? []
                  : values.forOrganizationIds,
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
      }}
    />
  );
};
