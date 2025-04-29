import { Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { SelfHostedEePlanCreateForm } from '../components/planForm/selfHostedEe/SelfHostedEePlanCreateForm';

export const AdministrationEePlanCreateView = () => {
  const { t } = useTranslate();

  return (
    <DashboardPage>
      <BaseAdministrationView
        windowTitle={t('administration_ee_plan_create')}
        navigation={[
          [
            t('administration_ee_plans'),
            LINKS.ADMINISTRATION_BILLING_EE_PLANS.build(),
          ],
          [
            t('administration_ee_plan_create'),
            LINKS.ADMINISTRATION_BILLING_EE_PLAN_CREATE.build(),
          ],
        ]}
        allCentered
        hideChildrenOnLoading={false}
      >
        <Typography variant="h5">
          {t('administration_ee_plan_create')}
        </Typography>
        <SelfHostedEePlanCreateForm />
      </BaseAdministrationView>
    </DashboardPage>
  );
};
