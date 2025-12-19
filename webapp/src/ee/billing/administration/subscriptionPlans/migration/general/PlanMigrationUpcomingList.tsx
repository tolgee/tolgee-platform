import { Alert, Box, TableCell, TableRow } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { UseQueryResult } from 'react-query';

import { EmptyState } from 'tg.component/common/EmptyState';
import { PaginatedHateoasTable } from 'tg.component/common/table/PaginatedHateoasTable';
import { HateoasListData } from 'tg.service/response.types';
import { components } from 'tg.service/billingApiSchema.generated';
import { PlanMigrationUpcomingItem } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/PlanMigrationUpcomingItem';
import React from 'react';

type UpcomingItem =
  components['schemas']['PlanMigrationUpcomingSubscriptionModel'];

type Props = {
  subscriptions: UseQueryResult<HateoasListData<UpcomingItem>>;
  setPage: (page: number) => void;
  onToggleSkip: (subscriptionId: number, skipped: boolean) => void;
  toggleLoading?: boolean;
  monthlyOffsetDays: number;
  yearlyOffsetDays: number;
};

export const PlanMigrationUpcomingList = ({
  subscriptions,
  setPage,
  onToggleSkip,
  toggleLoading,
}: Props) => {
  const { t } = useTranslate();

  return (
    <>
      <Box mb={1}>
        <Alert severity="info">
          <T keyName="administration_plan_migration_upcoming_hint" />
        </Alert>
      </Box>
      <Box data-cy="plan-migration-upcoming-list">
        <PaginatedHateoasTable
          wrapperComponentProps={{ className: 'listWrapper' }}
          loadable={subscriptions}
          onPageChange={setPage}
          tableHead={
            <TableRow>
              <TableCell>{t('global_organization')}</TableCell>
              <TableCell>{t('administration_plan_migration_from')}</TableCell>
              <TableCell>{t('administration_plan_migration_to')}</TableCell>
              <TableCell>
                {t('administration_plan_migration_expected_price')}
              </TableCell>
              <TableCell>
                {t('administration_plan_migration_scheduled_at')}
              </TableCell>
              <TableCell>
                {t('administration_plan_migration_skip_label')}
              </TableCell>
              <TableCell>
                {t('administration_plan_migrated_subscription_status')}
              </TableCell>
            </TableRow>
          }
          renderItem={(item) => (
            <PlanMigrationUpcomingItem
              subscription={item}
              onToggleSkip={onToggleSkip}
              toggleLoading={toggleLoading}
            />
          )}
          emptyPlaceholder={
            <EmptyState
              loading={subscriptions.isLoading}
              wrapperProps={{ py: 1 }}
            >
              <T keyName="administration_plan_migration_no_upcoming_subscriptions" />
            </EmptyState>
          }
        />
      </Box>
    </>
  );
};
