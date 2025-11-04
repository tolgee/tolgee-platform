import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { useHistory } from 'react-router-dom';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { PlanMigrationRecordList } from 'tg.ee.module/billing/administration/subscriptionPlans/migration/general/PlanMigrationRecordList';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { UseQueryResult } from 'react-query';
import { HateoasListData } from 'tg.service/response.types';
import { components } from 'tg.service/billingApiSchema.generated';
import { NavigationItem } from 'tg.component/navigation/Navigation';
import { ComponentType } from 'react';

type PlanMigrationRecord = components['schemas']['PlanMigrationRecordModel'];

type EditFormComponentProps<M> = {
  migration: M;
  onSubmit: () => void;
  onDelete?: () => void;
};

type Props<M> = {
  migrations: UseQueryResult<M>;
  subscriptions: UseQueryResult<HateoasListData<PlanMigrationRecord>>;
  navigation: NavigationItem[];
  listLink: string;
  form: ComponentType<EditFormComponentProps<M>>;
  onPage: (page: number) => void;
};

export const AdministrationPlanMigrationEditBase = <M,>({
  migrations,
  subscriptions,
  form: FormComponent,
  listLink,
  navigation,
  onPage,
}: Props<M>) => {
  const { t } = useTranslate();
  const messaging = useMessage();
  const history = useHistory();

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
          <Typography variant="h6">
            {t('administration_plan_migration_migrated_subscriptions')}
          </Typography>
        </Box>
        <PlanMigrationRecordList
          subscriptions={subscriptions}
          setPage={onPage}
        />
      </BaseAdministrationView>
    </DashboardPage>
  );
};
