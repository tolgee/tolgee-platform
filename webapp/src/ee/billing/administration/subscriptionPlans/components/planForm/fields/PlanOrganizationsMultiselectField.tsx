import React, { FC } from 'react';
import { Box, Typography } from '@mui/material';
import { CloudPlanOrganizations } from '../CloudPlanOrganizations';
import { useFormikContext } from 'formik';
import { CloudPlanFormData } from '../CloudPlanFormBase';

export const PlanOrganizationsMultiselectField: FC<{ editPlanId?: number }> = ({
  editPlanId,
}) => {
  const { values, setFieldValue, initialValues, errors } =
    useFormikContext<CloudPlanFormData>();

  return (
    <>
      {!values.public && (
        <Box>
          <CloudPlanOrganizations
            editPlanId={editPlanId}
            originalOrganizations={initialValues.forOrganizationIds}
            organizations={values.forOrganizationIds}
            setOrganizations={(orgs: number[]) => {
              setFieldValue('forOrganizationIds', orgs);
            }}
          />
          {errors.forOrganizationIds && (
            <Typography color="error">{errors.forOrganizationIds}</Typography>
          )}
        </Box>
      )}
    </>
  );
};
