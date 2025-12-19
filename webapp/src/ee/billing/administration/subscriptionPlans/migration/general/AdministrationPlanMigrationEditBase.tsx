import { Box, Tab, Tabs, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { useHistory } from 'react-router-dom';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { PlanMigrationRecordList } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/PlanMigrationRecordList';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { UseQueryResult } from 'react-query';
import { HateoasListData } from 'tg.service/response.types';
import { NavigationItem } from 'tg.component/navigation/Navigation';
import React, { ComponentType, useState } from 'react';
import { PlanMigrationUpcomingList } from './PlanMigrationUpcomingList';
import { components } from 'tg.service/billingApiSchema.generated';

type PlanMigrationRecord = components['schemas']['PlanMigrationRecordModel'];
type PlanMigration = {
  monthlyOffsetDays: number;
  yearlyOffsetDays: number;
};

type PlanMigrationUpcoming =
  components['schemas']['PlanMigrationUpcomingSubscriptionModel'];

type EditFormComponentProps<M> = {
  migration: M;
  onSubmit: () => void;
  onDelete?: () => void;
};

type Props<M> = {
  migrations: UseQueryResult<M>;
  subscriptions: UseQueryResult<HateoasListData<PlanMigrationRecord>>;
  upcomingSubscriptions: UseQueryResult<HateoasListData<PlanMigrationUpcoming>>;
  navigation: NavigationItem[];
  listLink: string;
  form: ComponentType<EditFormComponentProps<M>>;
  onPage: (page: number) => void;
  onUpcomingPage: (page: number) => void;
  onToggleUpcomingSkip: (subscriptionId: number, skipped: boolean) => void;
  upcomingToggleLoading?: boolean;
};

export const AdministrationPlanMigrationEditBase = <M extends PlanMigration>({
  migrations,
  subscriptions,
  upcomingSubscriptions,
  form: FormComponent,
  listLink,
  navigation,
  onPage,
  onUpcomingPage,
  onToggleUpcomingSkip,
  upcomingToggleLoading,
}: Props<M>) => {
  const { t } = useTranslate();
  const messaging = useMessage();
  const history = useHistory();
  const [tab, setTab] = useState(0);

  const onDelete = () => {
    messaging.success(
      <T keyName="administration_plan_migration_deleted_success" />
    );
    history.push(listLink);
  };

  const onSubmit = () => {
    messaging.success(
      <T keyName="administration_plan_migration_updated_success" />
    );
    history.push(listLink);
  };

  if (migrations.isLoading) {
    return <SpinnerProgress />;
  }

  const migration = migrations.data!;

  return (
    <DashboardPage>
      <BaseAdministrationView
        windowTitle={t('administration_plan_migration_configure_existing')}
        allCentered
        hideChildrenOnLoading={false}
        navigation={navigation}
      >
        <Box>
          <Typography variant="h5">
            {t('administration_plan_migration_configure_existing')}
          </Typography>
        </Box>
        <FormComponent
          migration={migration}
          onSubmit={onSubmit}
          onDelete={onDelete}
        />
        <Box my={2}>
          <Tabs
            value={tab}
            onChange={(_, value) => setTab(value)}
            aria-label="plan migration subscription tabs"
          >
            <Tab
              label={t('administration_plan_migration_upcoming_tab')}
              data-cy="plan-migration-tab-upcoming"
            />
            <Tab
              label={t('administration_plan_migration_migrated_tab')}
              data-cy="plan-migration-tab-migrated"
            />
          </Tabs>
        </Box>
        {tab === 0 && (
          <PlanMigrationUpcomingList
            subscriptions={upcomingSubscriptions}
            setPage={onUpcomingPage}
            onToggleSkip={onToggleUpcomingSkip}
            toggleLoading={upcomingToggleLoading}
            monthlyOffsetDays={migration.monthlyOffsetDays}
            yearlyOffsetDays={migration.yearlyOffsetDays}
          />
        )}
        {tab === 1 && (
          <>
            <PlanMigrationRecordList
              subscriptions={subscriptions}
              setPage={onPage}
            />
          </>
        )}
      </BaseAdministrationView>
    </DashboardPage>
  );
};
