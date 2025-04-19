import React, { FC } from 'react';
import { CloudPlanForm } from './CloudPlanForm';
import { T } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { getCloudPlanInitialValues } from './getCloudPlanInitialValues';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useHistory } from 'react-router-dom';
import { CreatingPlanForOrganizationAlert } from '../genericFields/CreatingPlanForOrganizationAlert';
import { CloudPlanTemplateSelectorField } from './fields/CloudPlanTemplateSelectorField';
import { CloudPlanFormData } from './types';
import { useCreatingForOrganization } from '../genericFields/useCreatingForOrganization';

export const CloudPlanCreateForm: FC = () => {
  const messaging = useMessage();
  const history = useHistory();

  const createPlanLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/cloud-plans',
    method: 'post',
  });

  const initialData = getCloudPlanInitialValues();

  function onSaveSuccess() {
    messaging.success(
      <T keyName="administration_cloud_plan_created_success" />
    );
    history.push(LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build());
  }

  const forOrganization = useCreatingForOrganization({
    initialPlanName: initialData.name,
  });

  initialData.name = forOrganization.initialPlanName;

  function onSubmit(values: CloudPlanFormData) {
    createPlanLoadable.mutate(
      {
        content: {
          'application/json': {
            ...values,
            stripeProductId: values.stripeProductId!,
            forOrganizationIds: forOrganization.id ? [forOrganization.id] : [],
          },
        },
      },
      {
        onSuccess: onSaveSuccess,
      }
    );
  }

  return (
    <CloudPlanForm
      loading={createPlanLoadable.isLoading}
      canEditPrices={true}
      onSubmit={onSubmit}
      initialData={initialData}
      publicSwitchFieldProps={{
        disabled: Boolean(forOrganization.id),
        disabledInfo: (
          <T keyName="admin_billing_disabled_public_switch_when_creating_cusom_tooltip" />
        ),
      }}
      beforeFields={
        <>
          <CreatingPlanForOrganizationAlert
            organization={forOrganization.data}
          />
          <CloudPlanTemplateSelectorField />
        </>
      }
    />
  );
};
