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

type CloudPlanMigrationModel = components['schemas']['CloudPlanMigrationModel'];
type SelfHostedEePlanMigrationModel =
  components['schemas']['AdministrationSelfHostedEePlanMigrationModel'];

type Props = {
  migration?: CloudPlanMigrationModel | SelfHostedEePlanMigrationModel;
  onSubmit: (value: PlanMigrationFormData) => void;
  onDelete?: (id: number) => void;
  planType?: PlanType;
  loading: boolean | undefined;
};

const emptyDefaultValues: PlanMigrationFormData = {
  enabled: true,
  sourcePlanId: 0,
  targetPlanId: 0,
  monthlyOffsetDays: 14,
  yearlyOffsetDays: 30,
};

export type PlanMigrationFormData =
  components['schemas']['PlanMigrationRequest'];

export const PlanMigrationForm = ({
  migration,
  onSubmit,
  loading,
  onDelete,
  planType = 'cloud',
}: Props) => {
  const { t } = useTranslate();
  const isUpdate = migration != null;
  const defaultValues: PlanMigrationFormData = migration
    ? {
        enabled: migration.enabled,
        sourcePlanId: migration.sourcePlan.id,
        targetPlanId: migration.targetPlan.id,
        monthlyOffsetDays: migration.monthlyOffsetDays,
        yearlyOffsetDays: migration.yearlyOffsetDays,
      }
    : emptyDefaultValues;

  const [selectedSourcePlan, setSelectedSourcePlan] = React.useState<number>(
    defaultValues.sourcePlanId
  );
  const [selectedTargetPlan, setSelectedTargetPlan] = React.useState<number>(
    defaultValues.targetPlanId
  );

  return (
    <Formik
      initialValues={defaultValues}
      enableReinitialize
      onSubmit={onSubmit}
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
            data-cy="source-plan-selector"
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
            data-cy="target-plan-selector"
            onPlanChange={(plan) => setSelectedTargetPlan(plan.id)}
            type={planType}
            hiddenPlans={[selectedSourcePlan]}
          />
        </Box>
        <Typography mb={1}>
          {t('administration_plan_migration_run_configuration')}
        </Typography>
        <Box display="grid" mb={2} width="fit-content">
          <TextField
            name="monthlyOffsetDays"
            type="number"
            label={t('administration_plan_migration_monthly_offset_days')}
            InputProps={{
              endAdornment: <Box>{t('global_days')}</Box>,
            }}
            required
          />
          <TextField
            name="yearlyOffsetDays"
            type="number"
            label={t('administration_plan_migration_yearly_offset_days')}
            InputProps={{
              endAdornment: <Box>{t('global_days')}</Box>,
            }}
            required
          />
        </Box>
        <Box display="flex" justifyContent="space-between" mt={4} gap={2}>
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
