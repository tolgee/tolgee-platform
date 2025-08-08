import { Form, Formik } from 'formik';
import { Box, Button, Typography } from '@mui/material';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import React from 'react';
import { T, useTranslate } from '@tolgee/react';
import { components } from 'tg.service/billingApiSchema.generated';
import { ArrowRightIcon } from '@mui/x-date-pickers';
import { PlanSelectorField } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/fields/PlanSelectorField';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Switch } from 'tg.component/common/form/fields/Switch';
import { PlanType } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/types';
import { confirmation } from 'tg.hooks/confirmation';
import { LabelHint } from 'tg.component/common/LabelHint';
import { Validation } from 'tg.constants/GlobalValidationSchema';

type CloudPlanMigrationModel = components['schemas']['CloudPlanMigrationModel'];
type SelfHostedEePlanMigrationModel =
  components['schemas']['AdministrationSelfHostedEePlanMigrationModel'];

type Props<T> = {
  defaultValues: T;
  onSubmit: (value: T) => void;
  onDelete?: (id: number) => void;
  planType?: PlanType;
  migration?: CloudPlanMigrationModel | SelfHostedEePlanMigrationModel;
  loading?: boolean;
};

export type PlanMigrationFormData =
  components['schemas']['PlanMigrationRequest'];

export type CreatePlanMigrationFormData =
  components['schemas']['CreatePlanMigrationRequest'];

export const PlanMigrationForm = <
  T extends CreatePlanMigrationFormData | PlanMigrationFormData
>({
  defaultValues,
  onSubmit,
  loading,
  onDelete,
  migration,
  planType = 'cloud',
}: Props<T>) => {
  const { t } = useTranslate();
  const isUpdate = migration != null;

  const [selectedSourcePlan, setSelectedSourcePlan] = React.useState<number>(
    (defaultValues as CreatePlanMigrationFormData).sourcePlanId
  );
  const [selectedTargetPlan, setSelectedTargetPlan] = React.useState<number>(
    defaultValues.targetPlanId
  );

  const initValues = {
    ...defaultValues,
    ...(isUpdate &&
      migration && {
        sourcePlanId: migration.sourcePlan.id,
      }),
  };

  return (
    <Formik<T>
      initialValues={initValues}
      enableReinitialize
      onSubmit={onSubmit}
      validationSchema={Validation.PLAN_MIRATION_FORM}
    >
      <Form>
        <Box mt={2}>
          <Switch name="enabled" label={t('global_enabled')} />
        </Box>
        <Box
          mb={3}
          gap={2}
          pt={2}
          display="grid"
          gridTemplateColumns="1fr auto 1fr"
          alignItems="center"
        >
          <PlanSelectorField
            name="sourcePlanId"
            selectProps={{
              label: t('administration_plan_migration_source_plan'),
              required: true,
              disabled: isUpdate,
            }}
            dataCy="source-plan-selector"
            onPlanChange={(plan) => setSelectedSourcePlan(plan.id)}
            hiddenPlans={[selectedTargetPlan]}
            filterHasMigration={false}
            type={planType}
            {...(migration && { plans: [migration.sourcePlan] })}
          />
          <ArrowRightIcon style={{ marginTop: 25 }} />
          <PlanSelectorField
            name="targetPlanId"
            selectProps={{
              label: t('administration_plan_migration_target_plan'),
              required: true,
            }}
            dataCy="target-plan-selector"
            onPlanChange={(plan) => setSelectedTargetPlan(plan.id)}
            type={planType}
            hiddenPlans={[selectedSourcePlan]}
          />
        </Box>
        <Box display="flex" alignItems="center" mb={1}>
          <LabelHint title={t('administration_plan_migration_timing_hint')}>
            <Typography>
              {t('administration_plan_migration_run_configuration')}
            </Typography>
          </LabelHint>
        </Box>

        <Box display="grid" mb={2} width="fit-content">
          <TextField
            name="monthlyOffsetDays"
            type="number"
            label={t('administration_plan_migration_monthly_offset_days')}
            InputProps={{
              endAdornment: <Box>{t('global_days')}</Box>,
            }}
          />
          <TextField
            name="yearlyOffsetDays"
            type="number"
            label={t('administration_plan_migration_yearly_offset_days')}
            InputProps={{
              endAdornment: <Box>{t('global_days')}</Box>,
            }}
          />
        </Box>
        <Box display="flex" justifyContent="space-between" mt={4} gap={2}>
          <Box>
            {migration && isUpdate && (
              <Button
                color="error"
                data-cy="delete-plan-migration-button"
                variant={'outlined'}
                onClick={() =>
                  confirmation({
                    onConfirm: () => onDelete?.(migration.id),
                    message: t(
                      'administration_plan_migration_delete_confirmation'
                    ),
                    confirmButtonText: t('global_delete_button'),
                  })
                }
              >
                <T keyName="administration_plan_migration_delete_button" />
              </Button>
            )}
          </Box>
          <LoadingButton
            variant="contained"
            color="primary"
            type="submit"
            data-cy="form-submit-button"
            loading={loading}
          >
            {isUpdate ? t('global_form_save') : t('global_form_create')}
          </LoadingButton>
        </Box>
      </Form>
    </Formik>
  );
};
