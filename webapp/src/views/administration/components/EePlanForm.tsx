import {
  Box,
  FormControl,
  InputLabel,
  Typography,
  Checkbox,
  FormControlLabel,
  Switch,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Field, FieldProps, Form, Formik } from 'formik';

import { TextField } from 'tg.component/common/form/fields/TextField';
import { SearchSelect } from 'tg.component/searchSelect/SearchSelect';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { EePlanOrganizations } from './EePlanOrganizations';

type SelfHostedEePlanRequest = components['schemas']['SelfHostedEePlanRequest'];
type EnabledFeature =
  components['schemas']['SelfHostedEePlanRequest']['enabledFeatures'][number];

type FormData = {
  name: string;
  prices: SelfHostedEePlanRequest['prices'];
  includedUsage: SelfHostedEePlanRequest['includedUsage'];
  stripeProductId: string | undefined;
  enabledFeatures: EnabledFeature[];
  public: boolean;
  forOrganizationIds: number[];
};

type Props = {
  planId?: number;
  initialData: FormData;
  onSubmit: (value: FormData) => void;
  loading: boolean | undefined;
};

export function EePlanForm({ planId, initialData, onSubmit, loading }: Props) {
  const { t } = useTranslate();

  const productsLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/stripe-products',
    method: 'get',
  });

  const featuresLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/features',
    method: 'get',
  });

  const products = productsLoadable.data?._embedded?.stripeProductModels;

  return (
    <Formik
      initialValues={{
        name: initialData.name,
        prices: initialData.prices,
        includedUsage: initialData.includedUsage,
        stripeProductId: initialData.stripeProductId,
        enabledFeatures: initialData.enabledFeatures,
        public: initialData.public,
        forOrganizationIds: initialData.forOrganizationIds,
      }}
      enableReinitialize
      onSubmit={onSubmit}
      validationSchema={Validation.EE_PLAN_FORM}
    >
      {({ values, errors, setFieldValue }) => (
        <Form>
          <Box mb={3}>
            <TextField
              sx={{ mt: 2 }}
              name="name"
              size="small"
              label={t('administration_ee_plan_field_name')}
              fullWidth
              data-cy="administration-ee-plan-field-name"
            />
            <Box display="grid" gap={2} sx={{ mt: 2 }}>
              <Field name="stripeProductId">
                {({ meta, field, form }: FieldProps) => (
                  <FormControl
                    variant="standard"
                    error={!!meta.error && meta.touched}
                    sx={{
                      display: 'grid',
                    }}
                    data-cy="administration-ee-plan-field-stripe-product"
                  >
                    <InputLabel sx={{ position: 'static' }}>
                      {t('administration_ee_plan_field_stripe_product')}
                    </InputLabel>
                    <Box>
                      <SearchSelect
                        compareFunction={(prompt, label) =>
                          label.toLowerCase().includes(prompt.toLowerCase())
                        }
                        SelectProps={{
                          size: 'small',
                          fullWidth: true,
                          variant: 'outlined',
                        }}
                        value={field.value}
                        onChange={(val) => form.setFieldValue(field.name, val)}
                        items={
                          products?.map(({ id, name }) => ({
                            value: id,
                            name: `${id} ${name}`,
                          })) || []
                        }
                      />
                    </Box>
                  </FormControl>
                )}
              </Field>
            </Box>
            <Typography sx={{ mt: 2 }}>
              {t('administration_ee_plan_form_prices_title')}
            </Typography>
            <Box display="flex" gap={2} sx={{ mt: 1 }}>
              <TextField
                name="prices.subscriptionMonthly"
                size="small"
                data-cy="administration-ee-plan-field-price-monthly"
                label={t('administration_ee_plan_field_price_monthly')}
                type="number"
                fullWidth
              />
              <TextField
                name="prices.subscriptionYearly"
                size="small"
                data-cy="administration-ee-plan-field-price-yearly"
                label={t('administration_ee_plan_field_price_yearly')}
                type="number"
                fullWidth
              />
              <TextField
                name="prices.perSeat"
                size="small"
                data-cy="administration-ee-plan-field-price-per-seat"
                label={t('administration_ee_plan_field_price_per_seat')}
                type="number"
                fullWidth
              />
            </Box>

            <Typography sx={{ mt: 2 }}>
              {t('administration_ee_plan_form_limits_title')}
            </Typography>
            <Box display="flex" gap={2} sx={{ mt: 1 }}>
              <TextField
                name="includedUsage.seats"
                size="small"
                type="number"
                fullWidth
                data-cy="administration-ee-plan-field-included-seats"
                label={t('administration_ee_plan_field_included_seats')}
              />
            </Box>
            <Box>
              <Typography sx={{ mt: 2 }}>
                {t('administration_ee_plan_form_features_title')}
              </Typography>
              <Field name="enabledFeatures">
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
                        data-cy="administration-ee-plan-field-feature"
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

            <FormControlLabel
              control={
                <Switch
                  checked={values.public}
                  onChange={() => setFieldValue('public', !values.public)}
                />
              }
              data-cy="administration-ee-plan-field-public"
              label={t('administration_ee_plan_field_public')}
            />

            {!values.public && (
              <Box>
                <EePlanOrganizations
                  planId={planId}
                  originalOrganizations={initialData.forOrganizationIds}
                  organizations={values.forOrganizationIds}
                  setOrganizations={(orgs: number[]) => {
                    setFieldValue('forOrganizationIds', orgs);
                  }}
                />
                {errors.forOrganizationIds && (
                  <Typography color="error">
                    {errors.forOrganizationIds}
                  </Typography>
                )}
              </Box>
            )}

            <Box display="flex" justifyContent="end" mt={4}>
              <LoadingButton
                loading={loading}
                variant="contained"
                color="primary"
                type="submit"
                data-cy="administration-ee-plan-submit-button"
              >
                {t('global_form_save')}
              </LoadingButton>
            </Box>
          </Box>
        </Form>
      )}
    </Formik>
  );
}
