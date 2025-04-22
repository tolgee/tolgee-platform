import React, { FC } from 'react';
import { T } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import { SelfHostedEePlanForm } from './SelfHostedEePlanForm';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useHistory } from 'react-router-dom';
import { CreatingPlanForOrganizationAlert } from '../genericFields/CreatingPlanForOrganizationAlert';
import { useCreatingForOrganization } from '../genericFields/useCreatingForOrganization';
import { getSelfHostedPlanInitialValues } from './getSelfHostedPlanInitialValues';
import { SelfHostedEePlanFormData } from '../cloud/types';
import { SelfHostedEePlanTemplateSelectorField } from './fields/SelfHostedEePlanTemplateSelectorField';

export const SelfHostedEePlanCreateForm: FC = () => {
  const messaging = useMessage();
  const history = useHistory();

  const createPlanMutation = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans',
    method: 'post',
  });

  const initialData = getSelfHostedPlanInitialValues();

  const forOrganization = useCreatingForOrganization({
    initialPlanName: initialData.name,
  });

  initialData.name = forOrganization.initialPlanName;

  function onSubmit(values: SelfHostedEePlanFormData) {
    createPlanMutation.mutate(
      {
        content: {
          'application/json': {
            ...values,
            stripeProductId: values.stripeProductId,
            forOrganizationIds: forOrganization.id ? [forOrganization.id] : [],
          },
        },
      },
      {
        onSuccess() {
          messaging.success(
            <T keyName="administration_ee_plan_created_success" />
          );
          history.push(LINKS.ADMINISTRATION_BILLING_EE_PLANS.build());
        },
      }
    );
  }

  return (
    <SelfHostedEePlanForm
      loading={createPlanMutation.isLoading}
      isUpdate={false}
      onSubmit={onSubmit}
      initialData={initialData}
      canEditPrices={true}
      beforeFields={
        <>
          <CreatingPlanForOrganizationAlert
            organization={forOrganization.data}
          />
          <SelfHostedEePlanTemplateSelectorField />
        </>
      }
    />
  );
};
