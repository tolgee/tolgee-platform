import React, { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AdministrationPlanMigrationEditBase } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/AdministrationPlanMigrationEditBase';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useRouteMatch } from 'react-router-dom';
import { CloudPlanEditPlanMigrationForm } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/CloudPlanEditPlanMigrationForm';

export const AdministrationCloudPlanMigrationEdit = () => {
  const { t } = useTranslate();
  const match = useRouteMatch();
  const migrationId = Number(match.params[PARAMS.PLAN_MIGRATION_ID]);
  const [subscriptionsPage, setSubscriptionsPage] = useState(0);

  const migrations = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans/migration/{migrationId}',
    method: 'get',
    path: { migrationId },
  });

  const subscriptions = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans/migration/{migrationId}/subscriptions',
    method: 'get',
    path: { migrationId },
    query: {
      page: subscriptionsPage,
      size: 10,
    },
    options: {
      keepPreviousData: true,
    },
  });

  return (
    <AdministrationPlanMigrationEditBase
      migrations={migrations}
      subscriptions={subscriptions}
      navigation={[
        [
          t('administration_cloud_plans'),
          LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build(),
        ],
        [
          t('administration_plan_migration_configure_existing'),
          LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_MIGRATION_EDIT.build({
            [PARAMS.PLAN_MIGRATION_ID]: migrationId,
          }),
        ],
      ]}
      listLink={LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build()}
      form={CloudPlanEditPlanMigrationForm}
      onPage={setSubscriptionsPage}
    />
  );
};
