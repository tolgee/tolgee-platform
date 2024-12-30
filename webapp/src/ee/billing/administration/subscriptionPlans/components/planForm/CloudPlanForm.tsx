import { Box } from '@mui/material';
import { CloudPlanFields } from './fields/CloudPlanFields';
import React, { ComponentProps, ReactNode } from 'react';
import { PlanPublicSwitchField } from './fields/PlanPublicSwitchField';
import { PlanOrganizationsMultiselectField } from './fields/PlanOrganizationsMultiselectField';
import { CloudPlanSaveButton } from './CloudPlanSaveButton';
import { CloudPlanFormBase, CloudPlanFormData } from './CloudPlanFormBase';

type Props = {
  editPlanId?: number;
  initialData: CloudPlanFormData;
  onSubmit: (value: CloudPlanFormData) => void;
  loading: boolean | undefined;
  canEditPrices: boolean;
  beforeFields?: ReactNode;
  publicSwitchFieldProps?: ComponentProps<typeof PlanPublicSwitchField>;
  showForOrganizationsMultiselect?: boolean;
};

export function CloudPlanForm({
  editPlanId,
  initialData,
  loading,
  canEditPrices,
  onSubmit,
  beforeFields,
  publicSwitchFieldProps,
  showForOrganizationsMultiselect,
}: Props) {
  return (
    <CloudPlanFormBase initialData={initialData} onSubmit={onSubmit}>
      {beforeFields}
      <Box mb={3} pt={2}>
        <PlanPublicSwitchField {...publicSwitchFieldProps} />

        <CloudPlanFields
          isUpdate={Boolean(editPlanId)}
          canEditPrices={canEditPrices}
        />

        {!(showForOrganizationsMultiselect === false) && (
          <PlanOrganizationsMultiselectField editPlanId={editPlanId} />
        )}

        <CloudPlanSaveButton loading={loading} />
      </Box>
    </CloudPlanFormBase>
  );
}
