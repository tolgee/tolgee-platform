import { useTranslate } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import { CreatePlanMigrationFormData } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/PlanMigrationForm';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import React from 'react';
import { AdministrationPlanMigrationCreateBase } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/AdministrationPlanMigrationCreateBase';

export const AdministrationSelfHostedEePlanMigrationCreate = () => {
  const { t } = useTranslate();

  const createMutation = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration',
    method: 'post',
  });

  const onSubmit = (
    values: CreatePlanMigrationFormData,
    callbacks: { onSuccess: () => void }
  ) => {
    createMutation.mutate(
      {
        content: { 'application/json': values },
      },
      {
        onSuccess: callbacks.onSuccess,
      }
    );
  };

  return (
    <AdministrationPlanMigrationCreateBase
      onSubmit={onSubmit}
      navigation={[
        [
          t('administration_ee_plans'),
          LINKS.ADMINISTRATION_BILLING_EE_PLANS.build(),
        ],
        [
          t('administration_plan_migration_configure'),
          LINKS.ADMINISTRATION_BILLING_EE_PLAN_MIGRATION_CREATE.build(),
        ],
      ]}
      successLink={LINKS.ADMINISTRATION_BILLING_EE_PLANS}
      isLoading={createMutation.isLoading}
      planType={'self-hosted'}
    />
  );
};
