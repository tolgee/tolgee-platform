import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { LINKS } from 'tg.constants/links';
import {
  CreatePlanMigrationFormData,
  PlanMigrationForm,
  PlanMigrationFormData,
} from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/PlanMigrationForm';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import React from 'react';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useHistory } from 'react-router-dom';

export const AdministrationCloudPlanMigrationCreate = () => {
  const { t } = useTranslate();
  const messaging = useMessage();
  const history = useHistory();

  const submit = (
    values: CreatePlanMigrationFormData | PlanMigrationFormData
  ) => {
    createPlanMigrationLoadable.mutate(
      {
        content: { 'application/json': values as CreatePlanMigrationFormData },
      },
      {
        onSuccess: () => {
          messaging.success(
            <T keyName="administration_plan_migration_created_success" />
          );
          history.push(LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build());
        },
      }
    );
  };

  const createPlanMigrationLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/cloud-plans/migration',
    method: 'post',
  });

  return (
    <DashboardPage>
      <BaseAdministrationView
        windowTitle={t('administration_plan_migration_configure')}
        allCentered
        hideChildrenOnLoading={false}
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
      >
        <Box>
          <Typography variant="h5">
            {t('administration_plan_migration_configure')}
          </Typography>
        </Box>
        <PlanMigrationForm
          onSubmit={submit}
          loading={createPlanMigrationLoadable.isLoading}
        />
      </BaseAdministrationView>
    </DashboardPage>
  );
};
