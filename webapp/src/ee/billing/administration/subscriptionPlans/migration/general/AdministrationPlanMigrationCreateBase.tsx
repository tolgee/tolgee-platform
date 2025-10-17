import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { Link } from 'tg.constants/links';
import { Box, Typography } from '@mui/material';
import { CreatePlanMigrationForm } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/CreatePlanMigrationForm';
import React from 'react';
import { T, useTranslate } from '@tolgee/react';
import { CreatePlanMigrationFormData } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/PlanMigrationForm';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useHistory } from 'react-router-dom';
import { NavigationItem } from 'tg.component/navigation/Navigation';
import { PlanType } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/types';

type Props = {
  onSubmit: (
    data: CreatePlanMigrationFormData,
    callbacks: { onSuccess: () => void }
  ) => void;
  navigation: NavigationItem[];
  successLink: Link;
  isLoading: boolean;
  planType?: PlanType;
};

export const AdministrationPlanMigrationCreateBase = ({
  onSubmit,
  navigation,
  successLink,
  isLoading,
  planType,
}: Props) => {
  const { t } = useTranslate();
  const messaging = useMessage();
  const history = useHistory();

  const submit = (values: CreatePlanMigrationFormData) => {
    onSubmit(values, {
      onSuccess: () => {
        messaging.success(
          <T keyName="administration_plan_migration_created_success" />
        );
        history.push(successLink.build());
      },
    });
  };

  return (
    <DashboardPage>
      <BaseAdministrationView
        windowTitle={t('administration_plan_migration_configure')}
        allCentered
        hideChildrenOnLoading={false}
        navigation={navigation}
      >
        <Box>
          <Typography variant="h5">
            {t('administration_plan_migration_configure')}
          </Typography>
        </Box>
        <CreatePlanMigrationForm
          onSubmit={submit}
          loading={isLoading}
          planType={planType}
        />
      </BaseAdministrationView>
    </DashboardPage>
  );
};
