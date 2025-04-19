import React, { FC } from 'react';
import { Box, Checkbox, FormControlLabel, Typography } from '@mui/material';
import { Field, FieldProps } from 'formik';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useTranslate } from '@tolgee/react';

type EnabledFeaturesFieldProps = {
  parentName?: string;
};

export const PlanEnabledFeaturesField: FC<EnabledFeaturesFieldProps> = ({
  parentName = '',
}) => {
  const featuresLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/features',
    method: 'get',
  });

  const { t } = useTranslate();

  return (
    <Box>
      <Typography sx={{ mt: 2 }}>
        {t('administration_cloud_plan_form_features_title')}
      </Typography>
      <Field name={`${parentName}enabledFeatures`}>
        {(props: FieldProps<string[]>) =>
          featuresLoadable.data?.map((feature) => {
            const values = props.field.value;

            const toggleField = () => {
              let newValues: string[];
              if (values.includes(feature)) {
                newValues = values.filter((val) => val !== feature);
              } else {
                newValues = [...values, feature];
              }
              props.form.setFieldValue(props.field.name, newValues);
            };

            return (
              <FormControlLabel
                data-cy="administration-plan-field-feature"
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
  );
};
