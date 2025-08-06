import { Box, Link, TableCell, TableRow, Typography } from '@mui/material';
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
import React, { useState } from 'react';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useHistory, useRouteMatch } from 'react-router-dom';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { PaginatedHateoasTable } from 'tg.component/common/table/PaginatedHateoasTable';
import { PlanMigrationStatus } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/PlanMigrationStatus';
import { EmptyState } from 'tg.component/common/EmptyState';
import { useDateFormatter } from 'tg.hooks/useLocale';

export const AdministrationSelfHostedEePlanMigrationEdit = () => {
  const { t } = useTranslate();
  const match = useRouteMatch();
  const messaging = useMessage();
  const history = useHistory();
  const formatDate = useDateFormatter();
  const migrationId = match.params[PARAMS.PLAN_MIGRATION_ID] as number;
  const [subscriptionsPage, setSubscriptionsPage] = useState(0);

  const migrationLoadable = useBillingApiQuery({
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

  const updatePlanMigrationLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}',
    method: 'put',
  });

  const deletePlanMigrationMutation = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}',
    method: 'delete',
  });

  const onDelete = (migrationId: number) => {
    deletePlanMigrationMutation.mutate(
      { path: { migrationId } },
      {
        onSuccess: () => {
          messaging.success(
            <T keyName="administration_plan_migration_deleted_success" />
          );
          history.push(LINKS.ADMINISTRATION_BILLING_EE_PLANS.build());
        },
      }
    );
  };

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
          history.push(LINKS.ADMINISTRATION_BILLING_EE_PLANS.build());
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
            LINKS.ADMINISTRATION_BILLING_EE_PLAN_MIGRATION_EDIT.build({
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
          onDelete={onDelete}
          loading={updatePlanMigrationLoadable.isLoading}
          planType="self-hosted"
        />
        <Box my={2}>
          <Typography variant="h6">
            {t('administration_plan_migration_migrated_subscriptions')}
          </Typography>
        </Box>
        <PaginatedHateoasTable
          wrapperComponentProps={{ className: 'listWrapper' }}
          loadable={subscriptions}
          onPageChange={setSubscriptionsPage}
          tableHead={
            <>
              <TableCell>{t('global_organization')}</TableCell>
              <TableCell>{t('administration_plan_migration_from')}</TableCell>
              <TableCell>{t('administration_plan_migration_to')}</TableCell>
              <TableCell>{t('administration_plan_migrated_at')}</TableCell>
              <TableCell>
                {t('administration_plan_migrated_subscription_status')}
              </TableCell>
            </>
          }
          renderItem={(item) => (
            <TableRow>
              <TableCell>
                <Link
                  href={LINKS.ORGANIZATION_PROFILE.build({
                    [PARAMS.ORGANIZATION_SLUG]: item.organizationSlug,
                  })}
                >
                  {item.organizationName}
                </Link>{' '}
              </TableCell>
              <TableCell>{item.originPlan}</TableCell>
              <TableCell>{item.plan}</TableCell>
              <TableCell>
                {formatDate(item.scheduledAt, {
                  timeZone: 'UTC',
                  dateStyle: 'short',
                  timeStyle: 'short',
                })}
              </TableCell>
              <TableCell>
                <PlanMigrationStatus status={item.status} />
              </TableCell>
            </TableRow>
          )}
          emptyPlaceholder={
            <EmptyState
              loading={subscriptions.isLoading}
              wrapperProps={{ py: 1 }}
            >
              <T keyName="administration_plan_migration_no_migrated_subscriptions" />
            </EmptyState>
          }
        />
      </BaseAdministrationView>
    </DashboardPage>
  );
};
