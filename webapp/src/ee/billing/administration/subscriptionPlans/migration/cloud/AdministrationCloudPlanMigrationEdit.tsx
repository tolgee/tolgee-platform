import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { LINKS, PARAMS } from 'tg.constants/links';
import {
  PlanMigrationForm,
  PlanMigrationFormData,
} from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/PlanMigrationForm';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import React from 'react';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useHistory, useRouteMatch } from 'react-router-dom';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';

export const AdministrationCloudPlanMigrationEdit = () => {
  const { t } = useTranslate();
  const match = useRouteMatch();
  const messaging = useMessage();
  const history = useHistory();
  const migrationId = match.params[PARAMS.PLAN_MIGRATION_ID] as number;

  const migrationLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans/migration/{migrationId}',
    method: 'get',
    path: { migrationId },
  });

  const updatePlanMigrationLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/cloud-plans/migration/{migrationId}',
    method: 'put',
  });

  if (migrationLoadable.isLoading) {
    return <SpinnerProgress />;
  }

  const migration = migrationLoadable.data!;

  const submit = (values: PlanMigrationFormData) => {
    updatePlanMigrationLoadable.mutate(
      {
        path: { migrationId },
        content: { 'application/json': values },
      },
      {
        onSuccess: () => {
          messaging.success(
            <T keyName="administration_plan_migration_updated_success" />
          );
          history.push(LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build());
        },
      }
    );
  };

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
        <PlanMigrationForm
          migration={migration}
          onSubmit={submit}
          loading={updatePlanMigrationLoadable.isLoading}
        />
      </BaseAdministrationView>
    </DashboardPage>
  );
};
