import { Box } from '@mui/material';
import { CloudPlanFields } from '../cloud/fields/CloudPlanFields';
import React, { ComponentProps, ReactNode } from 'react';
import { PlanPublicSwitchField } from './PlanPublicSwitchField';
import { PlanSaveButton } from './PlanSaveButton';
import { CloudPlanFormBase } from '../cloud/CloudPlanFormBase';
import { CloudPlanFormData } from '../cloud/types';

type Props = {
  isUpdate?: boolean;
  initialData: CloudPlanFormData;
  onSubmit: (value: CloudPlanFormData) => void;
  loading: boolean | undefined;
  canEditPrices: boolean;
  beforeFields?: ReactNode;
  publicSwitchFieldProps?: ComponentProps<typeof PlanPublicSwitchField>;
};

export function CloudPlanForm({
  isUpdate,
  initialData,
  loading,
  canEditPrices,
  onSubmit,
  beforeFields,
  publicSwitchFieldProps,
}: Props) {
  return (
    <CloudPlanFormBase initialData={initialData} onSubmit={onSubmit}>
      {beforeFields}
      <Box mb={3} pt={2}>
        <PlanPublicSwitchField {...publicSwitchFieldProps} />

        <CloudPlanFields isUpdate={isUpdate} canEditPrices={canEditPrices} />

        <PlanSaveButton loading={loading} />
      </Box>
    </CloudPlanFormBase>
  );
}
