import { FC } from 'react';
import { FormControlLabel, Switch } from '@mui/material';
import { useFormikContext } from 'formik';
import { CloudPlanFormData } from './CloudPlanForm';
import { useTranslate } from '@tolgee/react';

type AssignCheckboxProps = {
  organizationId: number;
};

export const AssignSwitchCheckbox: FC<AssignCheckboxProps> = (props) => {
  const { setFieldValue, values } = useFormikContext<CloudPlanFormData>();

  const value = values.autoAssignOrganizationIds;
  const checked = value.includes(props.organizationId);

  const { t } = useTranslate();

  const getNewValue = (value: number[], organizationId: number) => {
    if (value.includes(organizationId)) {
      return value.filter((id) => id !== organizationId);
    }
    return [...value, organizationId];
  };

  const isInList = values.forOrganizationIds.includes(props.organizationId);

  const onChange = () => {
    const newValue = getNewValue(value, props.organizationId);
    setFieldValue('autoAssignOrganizationIds', newValue);
  };

  return (
    <>
      {values.free && (
        <FormControlLabel
          sx={{ ml: 1 }}
          disabled={!isInList}
          control={<Switch checked={checked} onChange={onChange} />}
          data-cy="administration-cloud-plan-organization-assign-switch"
          label={t('administration_cloud_plan_organization_field_auto_assign')}
        />
      )}
    </>
  );
};
