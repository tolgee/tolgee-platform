import { Form, Formik } from 'formik';
import { Box, Button, InputAdornment, Stack, Typography } from '@mui/material';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import React, { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { components } from 'tg.service/billingApiSchema.generated';
import { PlanSelectorField } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/fields/PlanSelectorField';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Switch } from 'tg.component/common/form/fields/Switch';
import { PlanType } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/types';
import { confirmation } from 'tg.hooks/confirmation';
import { LabelHint } from 'tg.component/common/LabelHint';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { ChevronRight } from '@untitled-ui/icons-react';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { PlanMigrationEmailSection } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/PlanMigrationEmailSection';

type CloudPlanMigrationModel = components['schemas']['CloudPlanMigrationModel'];
type SelfHostedEePlanMigrationModel =
  components['schemas']['AdministrationSelfHostedEePlanMigrationModel'];

export type EmailTemplateData = components['schemas']['EmailTemplateModel'];

const normalizeCustomBody = (
  body: string | null | undefined,
  template?: EmailTemplateData
): string | null | undefined => {
  if (!body || !body.trim()) {
    return null;
  }
  if (template && body === template.body) {
    return null;
  }
  return body;
};

type Props<T> = {
  defaultValues: T;
  onSubmit: (value: T) => void;
  onDelete?: (id: number) => void;
  planType?: PlanType;
  migration?: CloudPlanMigrationModel | SelfHostedEePlanMigrationModel;
  loading?: boolean;
};

export type PlanMigrationFormData =
  components['schemas']['PlanMigrationRequest'] & {
    sourcePlanFree: boolean;
    sourcePlanId?: number;
  };

export type CreatePlanMigrationFormData =
  components['schemas']['CreatePlanMigrationRequest'];

type FormPlanType = {
  id: number;
  free?: boolean;
};

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
  const messaging = useMessage();
  const isUpdate = migration != null;

  const defaultSourcePlan = migration
    ? {
        id: migration.sourcePlan.id,
        free: migration.sourcePlan.free,
      }
    : undefined;

  const [selectedSourcePlan, setSelectedSourcePlan] = useState<
    FormPlanType | undefined
  >(defaultSourcePlan);

  const [selectedTargetPlan, setSelectedTargetPlan] = useState<FormPlanType>({
    id: defaultValues.targetPlanId,
  });

  const templateQuery = useBillingApiQuery({
    url: '/v2/administration/billing/plan-migration/email-template',
    method: 'get',
  });

  const cloudPreviewMutation = useBillingApiMutation({
    url: '/v2/administration/billing/cloud-plans/migration/email-preview',
    method: 'post',
  });

  const selfHostedPreviewMutation = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/email-preview',
    method: 'post',
  });

  const previewMutation =
    planType === 'cloud' ? cloudPreviewMutation : selfHostedPreviewMutation;

  const templateData: EmailTemplateData | undefined = templateQuery.data;

  const initValues = {
    ...defaultValues,
    ...(isUpdate &&
      migration && {
        sourcePlanId: migration.sourcePlan.id,
        customEmailBody: migration.customEmailBody ?? null,
      }),
  } as T;

  const sendPreview = (values: T) => {
    if (!values.sourcePlanId || !values.targetPlanId) {
      return;
    }
    const body = normalizeCustomBody(values.customEmailBody, templateData);
    previewMutation.mutate(
      {
        content: {
          'application/json': {
            sourcePlanId: values.sourcePlanId,
            targetPlanId: values.targetPlanId,
            customEmailBody: body ?? undefined,
          },
        },
      },
      {
        onSuccess: () =>
          messaging.success(
            <T keyName="administration_plan_migration_preview_sent" />
          ),
      }
    );
  };

  if (!templateData) {
    return <SpinnerProgress />;
  }

  const handleSubmit = (vals: T) => {
    const normalizedBody = normalizeCustomBody(
      vals.customEmailBody,
      templateData
    );
    onSubmit({ ...vals, customEmailBody: normalizedBody });
  };

  return (
    <Formik<T>
      initialValues={initValues}
      enableReinitialize
      onSubmit={handleSubmit}
      validationSchema={Validation.PLAN_MIGRATION_FORM}
    >
      {({ values }) => {
        const previewDisabled =
          !values.sourcePlanId || !values.targetPlanId || !templateData;

        return (
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
                onPlanChange={(plan) => {
                  setSelectedSourcePlan(plan);
                }}
                planProps={{
                  hiddenIds: [selectedTargetPlan.id],
                }}
                filterHasMigration={false}
                type={planType}
                {...(isUpdate && { plans: [migration.sourcePlan] })}
              />
              <ChevronRight style={{ marginTop: 25 }} />
              <PlanSelectorField
                name="targetPlanId"
                selectProps={{
                  label: t('administration_plan_migration_target_plan'),
                  required: true,
                }}
                dataCy="target-plan-selector"
                onPlanChange={(plan) => setSelectedTargetPlan(plan)}
                type={planType}
                planProps={
                  selectedSourcePlan && {
                    hiddenIds: [selectedSourcePlan.id],
                    free: selectedSourcePlan.free,
                  }
                }
              />
            </Box>
            <Box display="flex" alignItems="center" mb={1}>
              <LabelHint title={t('administration_plan_migration_timing_hint')}>
                <Typography>
                  {t('administration_plan_migration_run_configuration')}
                </Typography>
              </LabelHint>
            </Box>

            <Box display="grid" width="fit-content">
              <TextField
                name="monthlyOffsetDays"
                type="number"
                label={t('administration_plan_migration_monthly_offset_days')}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position={'end'}>
                      <Box>{t('global_days')}</Box>
                    </InputAdornment>
                  ),
                }}
              />
              <TextField
                name="yearlyOffsetDays"
                type="number"
                label={t('administration_plan_migration_yearly_offset_days')}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position={'end'}>
                      <Box>{t('global_days')}</Box>
                    </InputAdornment>
                  ),
                }}
              />
            </Box>

            <PlanMigrationEmailSection template={templateData} />

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
              <Stack direction="row" spacing={1}>
                <LoadingButton
                  variant="outlined"
                  color="primary"
                  type="button"
                  data-cy="send-preview-email"
                  loading={previewMutation.isLoading}
                  disabled={previewDisabled || previewMutation.isLoading}
                  onClick={() => sendPreview(values)}
                >
                  {t('administration_plan_migration_send_preview')}
                </LoadingButton>
                <LoadingButton
                  variant="contained"
                  color="primary"
                  type="submit"
                  data-cy="form-submit-button"
                  loading={loading}
                >
                  {isUpdate ? t('global_form_save') : t('global_form_create')}
                </LoadingButton>
              </Stack>
            </Box>
          </Form>
        );
      }}
    </Formik>
  );
};
