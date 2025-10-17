import { useTranslate } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import React from 'react';
import { AdministrationPlanMigrationCreateBase } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/AdministrationPlanMigrationCreateBase';
import { CreatePlanMigrationFormData } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/PlanMigrationForm';

export const AdministrationCloudPlanMigrationCreate = () => {
  const { t } = useTranslate();

  const onSubmit = (
    values: CreatePlanMigrationFormData,
    callbacks: { onSuccess: () => void }
  ) => {
    createMutation.mutate(
      {
        content: { 'application/json': values },
      },
      {
        onSuccess: callbacks?.onSuccess,
      }
    );
  };

  const createMutation = useBillingApiMutation({
    url: '/v2/administration/billing/cloud-plans/migration',
    method: 'post',
  });

  return (
    <AdministrationPlanMigrationCreateBase
      onSubmit={onSubmit}
      navigation={[
        [
          t('administration_cloud_plans'),
          LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build(),
        ],
        [
          t('administration_plan_migration_configure'),
          LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_MIGRATION_CREATE.build(),
        ],
      ]}
      successLink={LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS}
      isLoading={createMutation.isLoading}
    />
  );
};
