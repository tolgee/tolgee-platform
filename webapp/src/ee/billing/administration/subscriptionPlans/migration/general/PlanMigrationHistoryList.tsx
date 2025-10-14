import { Link, TableCell, TableRow } from '@mui/material';
import { LINKS, PARAMS } from 'tg.constants/links';
import { PlanMigrationStatus } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/PlanMigrationStatus';
import { EmptyState } from 'tg.component/common/EmptyState';
import { T, useTranslate } from '@tolgee/react';
import { PaginatedHateoasTable } from 'tg.component/common/table/PaginatedHateoasTable';
import React from 'react';
import { UseQueryResult } from 'react-query';
import { components } from 'tg.service/billingApiSchema.generated';
import { HateoasListData } from 'tg.service/response.types';
import { useDateFormatter } from 'tg.hooks/useLocale';

type PlanMigrationHistoryModel =
  components['schemas']['PlanMigrationHistoryModel'];

type Props = {
  subscriptions: UseQueryResult<HateoasListData<PlanMigrationHistoryModel>>;
  setPage: (page: number) => void;
};

export const PlanMigrationHistoryList = ({ subscriptions, setPage }: Props) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();

  return (
    <PaginatedHateoasTable
      wrapperComponentProps={{ className: 'listWrapper' }}
      loadable={subscriptions}
      onPageChange={setPage}
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
            </Link>
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
            <PlanMigrationStatus status={item.status} date={item.finalizedAt} />
          </TableCell>
        </TableRow>
      )}
      emptyPlaceholder={
        <EmptyState loading={subscriptions.isLoading} wrapperProps={{ py: 1 }}>
          <T keyName="administration_plan_migration_no_migrated_subscriptions" />
        </EmptyState>
      }
    />
  );
};
