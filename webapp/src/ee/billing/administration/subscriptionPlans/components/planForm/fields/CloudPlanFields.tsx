import { TextField } from 'tg.component/common/form/fields/TextField';
import { useTranslate } from '@tolgee/react';
import {
  Box,
  Checkbox,
  FormControlLabel,
  MenuItem,
  Switch,
  Typography,
} from '@mui/material';
import { Select } from 'tg.component/common/form/fields/Select';
import { Field, FieldProps, useFormikContext } from 'formik';
import { SearchSelect } from 'tg.component/searchSelect/SearchSelect';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import React, { FC, useEffect } from 'react';
import { CloudPlanPricesAndLimits } from './CloudPlanPricesAndLimits';
import { CloudPlanFormData } from '../CloudPlanFormBase';
import { PlanNonCommercialSwitch } from './PlanNonCommercialSwitch';

export const CloudPlanFields: FC<{
  parentName?: string;
  isUpdate: boolean;
  canEditPrices: boolean;
}> = ({ parentName, isUpdate, canEditPrices }) => {
  const { t } = useTranslate();

  const featuresLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/features',
    method: 'get',
  });

  const productsLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/stripe-products',
    method: 'get',
  });

  const products = productsLoadable.data?._embedded?.stripeProducts;

  const { setFieldValue, values: formValues } = useFormikContext<any>();

  const values: CloudPlanFormData = parentName
    ? formValues[parentName]
    : formValues;

  parentName = parentName ? parentName + '.' : '';

  const typeOptions = [
    { value: 'PAY_AS_YOU_GO', label: 'Pay as you go', enabled: !values.free },
    { value: 'FIXED', label: 'Fixed', enabled: true },
    { value: 'SLOTS_FIXED', label: 'Slots fixed', enabled: true },
  ];

  const enabledTypeOptions = typeOptions.filter((t) => t.enabled);

  function onFreeChange() {
    setFieldValue(`${parentName}free`, !values.free);
  }

  useEffect(() => {
    if (!enabledTypeOptions.find((o) => o.value === values.type)) {
      setFieldValue(`${parentName}type`, enabledTypeOptions[0].value);
    }
  }, [values.free]);

  return (
    <>
      <TextField
        name={`${parentName}name`}
        size="small"
        label={t('administration_cloud_plan_field_name')}
        fullWidth
        data-cy="administration-cloud-plan-field-name"
      />
      <FormControlLabel
        disabled={isUpdate}
        control={
          <Switch checked={values.free} onChange={() => onFreeChange()} />
        }
        data-cy="administration-cloud-plan-field-free"
        label={t('administration_cloud_plan_field_free')}
      />
      <Box
        sx={{
          display: 'grid',
          gap: 2,
          mt: 2,
          gridTemplateColumns: '1fr 1fr',
        }}
      >
        <Select
          label={t('administration_cloud_plan_field_type')}
          name={`${parentName}type`}
          size="small"
          fullWidth
          minHeight={false}
          sx={{ flexBasis: '50%' }}
          data-cy="administration-cloud-plan-field-type"
          renderValue={(val) =>
            enabledTypeOptions.find((o) => o.value === val)?.label
          }
        >
          {enabledTypeOptions.map(({ value, label }) => (
            <MenuItem
              key={value}
              value={value}
              data-cy="administration-cloud-plan-field-type-item"
            >
              {label}
            </MenuItem>
          ))}
        </Select>
        <Field name={`${parentName}stripeProductId`}>
          {({ field, form, meta }: FieldProps) => {
            return (
              <SearchSelect
                compareFunction={(prompt, label) =>
                  label.toLowerCase().includes(prompt.toLowerCase())
                }
                SelectProps={{
                  // @ts-ignore
                  'data-cy': 'administration-cloud-plan-field-stripe-product',
                  label: t('administration_cloud_plan_field_stripe_product'),
                  size: 'small',
                  fullWidth: true,
                  variant: 'outlined',
                  error: (meta.touched && meta.error) || '',
                }}
                value={field.value}
                onChange={(val) => form.setFieldValue(field.name, val)}
                items={[
                  { value: '', name: 'None' },
                  ...(products?.map(({ id, name }) => ({
                    value: id,
                    name: `${id} ${name}`,
                  })) || []),
                ]}
              />
            );
          }}
        </Field>
      </Box>

      <CloudPlanPricesAndLimits
        parentName={parentName}
        values={values}
        canEditPrices={canEditPrices}
      />

      <Box>
        <Typography sx={{ mt: 2 }}>
          {t('administration_cloud_plan_form_features_title')}
        </Typography>
        <Field name={`${parentName}enabledFeatures`}>
          {(props: FieldProps<string[]>) =>
            featuresLoadable.data?.map((feature) => {
              const values = props.field.value;

              const toggleField = () => {
                let newValues = values;
                if (values.includes(feature)) {
                  newValues = values.filter((val) => val !== feature);
                } else {
                  newValues = [...values, feature];
                }
                props.form.setFieldValue(props.field.name, newValues);
              };

              return (
                <FormControlLabel
                  data-cy="administration-cloud-plan-field-feature"
                  key={feature}
                  control={
                    <Checkbox
                      value={feature}
                      checked={props.field.value.includes(feature)}
                      onChange={toggleField}
                    />
                  }
                  label={feature}
                />
              );
            }) || []
          }
        </Field>
      </Box>
      <PlanNonCommercialSwitch />
    </>
  );
};
