import React, { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AdministrationPlanMigrationEditBase } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/AdministrationPlanMigrationEditBase';
import { SelfHostedEePlanEditPlanMigrationForm } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/SelfHostedEePlanEditPlanMigrationForm';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useRouteMatch } from 'react-router-dom';

export const AdministrationSelfHostedEePlanMigrationEdit = () => {
  const { t } = useTranslate();
  const match = useRouteMatch();
  const migrationId = Number(match.params[PARAMS.PLAN_MIGRATION_ID]);
  const [subscriptionsPage, setSubscriptionsPage] = useState(0);
  const migrations = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}',
    method: 'get',
    path: { migrationId },
  });

  const subscriptions = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}/subscriptions',
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
          t('administration_ee_plans'),
          LINKS.ADMINISTRATION_BILLING_EE_PLANS.build(),
        ],
        [
          t('administration_plan_migration_configure_existing'),
          LINKS.ADMINISTRATION_BILLING_EE_PLAN_MIGRATION_EDIT.build({
            [PARAMS.PLAN_MIGRATION_ID]: migrationId,
          }),
        ],
      ]}
      listLink={LINKS.ADMINISTRATION_BILLING_EE_PLANS.build()}
      form={SelfHostedEePlanEditPlanMigrationForm}
      onPage={setSubscriptionsPage}
    />
  );
};
