import React, { FC } from 'react';
import { CloudPlanForm } from './CloudPlanForm';
import { T } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import {
  useApiQuery,
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { getCloudPlanInitialValues } from './getCloudPlanInitialValues';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useHistory } from 'react-router-dom';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { Alert } from '@mui/material';

export const CloudPlanEditForm: FC<{ planId: number }> = ({ planId }) => {
  const messaging = useMessage();
  const history = useHistory();

  const { editingForOrganizationId: editingForOrganizationIdString } =
    useUrlSearch();

  const editingForOrganizationId = editingForOrganizationIdString
    ? parseInt(editingForOrganizationIdString as string)
    : undefined;

  const planLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans/{planId}',
    method: 'get',
    path: { planId },
  });

  const planEditLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/cloud-plans/{planId}',
    method: 'put',
    invalidatePrefix: '/v2/administration/billing/cloud-plans',
  });

  if (planLoadable.isLoading) {
    return <SpinnerProgress />;
  }

  const planData = planLoadable.data;

  if (!planData) {
    return null;
  }

  const initialData = getCloudPlanInitialValues(planData);

  const NotExclusiveAlert = () => {
    const organizationLoadable = useApiQuery({
      url: '/v2/organizations/{id}',
      method: 'get',
      path: { id: editingForOrganizationId || 0 },
      options: {
        enabled: !!editingForOrganizationId,
      },
    });

    if (!editingForOrganizationId) {
      return null;
    }

    const isExclusive =
      planData.exclusiveForOrganizationId === editingForOrganizationId;

    if (isExclusive || !organizationLoadable.data) {
      return null;
    }

    return (
      <Alert severity="warning" sx={{ mt: 2 }}>
        <T
          keyName={'admin_billing_plan_edit_not_exclusive_warning'}
          params={{
            organizationName: organizationLoadable.data?.name,
            b: <b />,
          }}
        />
      </Alert>
    );
  };

  return (
    <CloudPlanForm
      isUpdate={true}
      beforeFields={<NotExclusiveAlert />}
      loading={planEditLoadable.isLoading}
      canEditPrices={planLoadable.data?.canEditPrices || false}
      initialData={initialData}
      publicSwitchFieldProps={{
        disabled:
          !!planData.exclusiveForOrganizationId || !!editingForOrganizationId,
        disabledInfo: (
          <T keyName="administration_billing_custom_plan_public_state_disabled_info" />
        ),
      }}
      onSubmit={(values) => {
        planEditLoadable.mutate(
          {
            path: { planId },
            content: {
              'application/json': {
                ...values,
                stripeProductId: values.stripeProductId ?? '',
                forOrganizationIds: values.public
                  ? []
                  : values.forOrganizationIds,
              },
            },
          },
          {
            onSuccess() {
              messaging.success(
                <T keyName="administration_cloud_plan_updated_success" />
              );
              history.push(LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build());
            },
          }
        );
      }}
    />
  );
};
