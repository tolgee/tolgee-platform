import { PlanMigrationFormData } from './PlanMigrationForm';
import { components } from 'tg.service/billingApiSchema.generated';
import { EditPlanMigrationForm } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/EditPlanMigrationForm';
import React from 'react';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';

type SelfHostedEePlanMigrationModel =
  components['schemas']['AdministrationSelfHostedEePlanMigrationModel'];

type Props = {
  migration: SelfHostedEePlanMigrationModel;
  onSubmit: () => void;
  onDelete?: () => void;
};

export const SelfHostedEePlanEditPlanMigrationForm = ({
  migration,
  onSubmit,
  onDelete,
}: Props) => {
  const updateLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}',
    method: 'put',
  });

  const deleteLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}',
    method: 'delete',
  });

  const remove = (migrationId: number) => {
    deleteLoadable.mutate(
      { path: { migrationId } },
      {
        onSuccess: onDelete,
      }
    );
  };

  const submit = (values: PlanMigrationFormData) => {
    updateLoadable.mutate(
      {
        path: { migrationId: migration.id },
        content: { 'application/json': values },
      },
      {
        onSuccess: onSubmit,
      }
    );
  };

  return (
    <EditPlanMigrationForm
      migration={migration}
      onSubmit={submit}
      onDelete={remove}
      loading={updateLoadable.isLoading}
      planType={'self-hosted'}
    />
  );
};
