import React, { FC } from 'react';
import { FormControlLabel, Switch as MuiSwitch, Tooltip } from '@mui/material';
import { useFormikContext } from 'formik';
import { AssignCloudPlanValuesType } from '../AssignCloudPlanDialog';
import { useTranslate } from '@tolgee/react';

type AssignCloudPlanTrialSwitchFieldProps = {
  isCurrentlyPaying: boolean;
  defaultTrialDate: Date;
};

export const AssignCloudPlanTrialSwitchField: FC<
  AssignCloudPlanTrialSwitchFieldProps
> = ({ isCurrentlyPaying, defaultTrialDate }) => {
  const { t } = useTranslate();
  const formikProps = useFormikContext<AssignCloudPlanValuesType>();

  return (
    <FormControlLabel
      control={
        <Tooltip
          title={
            isCurrentlyPaying
              ? t('assign-plan-trial-disabled-tooltip')
              : undefined
          }
        >
          <span>
            <MuiSwitch
              data-cy="administration-assign-plan-dialog-trial-switch"
              disabled={isCurrentlyPaying}
              checked={formikProps.values.trialEnd !== undefined}
              onChange={(_, checked) => {
                checked
                  ? formikProps.setFieldValue('trialEnd', defaultTrialDate)
                  : formikProps.setFieldValue('trialEnd', undefined);
              }}
            />
          </span>
        </Tooltip>
      }
      label={t('administration-assign-plan-dialog-trial-switch')}
    />
  );
};
