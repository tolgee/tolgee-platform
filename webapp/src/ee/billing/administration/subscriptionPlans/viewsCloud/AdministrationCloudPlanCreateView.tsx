import { Box, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { CloudPlanCreateForm } from '../components/planForm/cloud/CloudPlanCreateForm';

export const AdministrationCloudPlanCreateView = () => {
  const { t } = useTranslate();

  return (
    <DashboardPage>
      <BaseAdministrationView
        windowTitle={t('administration_cloud_plan_create')}
        navigation={[
          [
            t('administration_cloud_plans'),
            LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build(),
          ],
          [
            t('administration_cloud_plan_create'),
            LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_CREATE.build(),
          ],
        ]}
        allCentered
        hideChildrenOnLoading={false}
      >
        <Box>
          <Typography variant="h5">
            {t('administration_cloud_plan_create')}
          </Typography>

          <CloudPlanCreateForm />
        </Box>
      </BaseAdministrationView>
    </DashboardPage>
  );
};
