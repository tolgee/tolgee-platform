import { Box, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { CloudPlanEditForm } from '../components/planForm/cloud/CloudPlanEditForm';

export const AdministrationCloudPlanEditView = () => {
  const match = useRouteMatch();
  const { t } = useTranslate();

  const planId = match.params[PARAMS.PLAN_ID];

  return (
    <DashboardPage>
      <BaseAdministrationView
        windowTitle={t('administration_cloud_plan_edit')}
        navigation={[
          [
            t('administration_cloud_plans'),
            LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build(),
          ],
          [
            t('administration_cloud_plan_edit'),
            LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_EDIT.build({
              [PARAMS.PLAN_ID]: planId,
            }),
          ],
        ]}
        allCentered
        hideChildrenOnLoading={false}
      >
        <Box>
          <Typography variant="h5">
            {t('administration_cloud_plan_edit')}
          </Typography>
        </Box>
        <CloudPlanEditForm planId={planId} />
      </BaseAdministrationView>
    </DashboardPage>
  );
};
