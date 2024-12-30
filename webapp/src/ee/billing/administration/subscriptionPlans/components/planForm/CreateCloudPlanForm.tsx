import React, { FC } from 'react';
import { CloudPlanForm } from './CloudPlanForm';
import { T } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import {
  useApiQuery,
  useBillingApiMutation,
} from 'tg.service/http/useQueryApi';
import { getCloudPlanInitialValues } from './getCloudPlanInitialValues';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useHistory } from 'react-router-dom';
import { CloudPlanFormData } from './CloudPlanFormBase';
import { CreatingPlanForOrganizationAlert } from './CreatingPlanForOrganizationAlert';
import { PlanTemplateSelector } from './fields/PlanTemplateSelector';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';

export const CreateCloudPlanForm: FC = () => {
  const messaging = useMessage();
  const history = useHistory();

  const createPlanLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/cloud-plans',
    method: 'post',
  });

  const initialData = getCloudPlanInitialValues();

  function onSubmit(values: CloudPlanFormData) {
    createPlanLoadable.mutate(
      {
        content: {
          'application/json': {
            ...values,
            stripeProductId: values.stripeProductId!,
            forOrganizationIds: values.public ? [] : values.forOrganizationIds,
          },
        },
      },
      {
        onSuccess: onSaveSuccess,
      }
    );
  }

  function onSaveSuccess() {
    messaging.success(
      <T keyName="administration_cloud_plan_created_success" />
    );
    history.push(LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build());
  }

  const { creatingForOrganizationId: creatingForOrganizationIdString } =
    useUrlSearch();

  const creatingForOrganizationId = creatingForOrganizationIdString
    ? parseInt(creatingForOrganizationIdString as string)
    : undefined;

  const organizationLoadable = useApiQuery({
    url: '/v2/organizations/{id}',
    method: 'get',
    path: { id: creatingForOrganizationId || 0 },
    options: {
      enabled: !!creatingForOrganizationId,
    },
  });

  if (!initialData.name && organizationLoadable.data?.name) {
    initialData.name = 'Custom for ' + organizationLoadable.data.name;
  }

  return (
    <CloudPlanForm
      loading={createPlanLoadable.isLoading}
      canEditPrices={true}
      onSubmit={onSubmit}
      initialData={initialData}
      showForOrganizationsMultiselect={!creatingForOrganizationId}
      publicSwitchFieldProps={{
        disabled: Boolean(creatingForOrganizationId),
        disabledInfo: (
          <T keyName="admin_billing_disabled_public_switch_when_creating_cusom_tooltip" />
        ),
      }}
      beforeFields={
        <>
          <CreatingPlanForOrganizationAlert
            organization={organizationLoadable.data}
          />
          <PlanTemplateSelector />
        </>
      }
    />
  );
};
