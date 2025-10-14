import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import React, { useState } from 'react';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useHistory, useRouteMatch } from 'react-router-dom';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { PlanMigrationHistoryList } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/PlanMigrationHistoryList';
import { CloudPlanEditPlanMigrationForm } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/CloudPlanEditPlanMigrationForm';

export const AdministrationCloudPlanMigrationEdit = () => {
  const { t } = useTranslate();
  const match = useRouteMatch();
  const messaging = useMessage();
  const history = useHistory();
  const migrationId = match.params[PARAMS.PLAN_MIGRATION_ID] as number;
  const [subscriptionsPage, setSubscriptionsPage] = useState(0);

  const migrationLoadable = useBillingApiQuery({
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

  const onDelete = () => {
    messaging.success(
      <T keyName="administration_plan_migration_deleted_success" />
    );
    history.push(LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build());
  };

  const onSubmit = () => {
    messaging.success(
      <T keyName="administration_plan_migration_updated_success" />
    );
    history.push(LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build());
  };

  if (migrationLoadable.isLoading) {
    return <SpinnerProgress />;
  }

  const migration = migrationLoadable.data!;

  return (
    <DashboardPage>
      <BaseAdministrationView
        windowTitle={t('administration_plan_migration_configure_existing')}
        allCentered
        hideChildrenOnLoading={false}
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
      >
        <Box>
          <Typography variant="h5">
            {t('administration_plan_migration_configure_existing')}
          </Typography>
        </Box>
        <CloudPlanEditPlanMigrationForm
          migration={migration}
          onSubmit={onSubmit}
          onDelete={onDelete}
        />
        <Box my={2}>
          <Typography variant="h6">
            {t('administration_plan_migration_migrated_subscriptions')}
          </Typography>
        </Box>
        <PlanMigrationHistoryList
          subscriptions={subscriptions}
          setPage={setSubscriptionsPage}
        />
      </BaseAdministrationView>
    </DashboardPage>
  );
};
