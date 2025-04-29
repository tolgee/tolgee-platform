import { Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { SelfHostedEePlanEditForm } from '../components/planForm/selfHostedEe/SelfHostedEePlanEditForm';

export const AdministrationEePlanEditView = () => {
  const match = useRouteMatch();
  const { t } = useTranslate();

  const planId = match.params[PARAMS.PLAN_ID];

  return (
    <DashboardPage>
      <BaseAdministrationView
        windowTitle={t('administration_ee_plan_edit')}
        navigation={[
          [
            t('administration_ee_plans'),
            LINKS.ADMINISTRATION_BILLING_EE_PLANS.build(),
          ],
          [
            t('administration_ee_plan_edit'),
            LINKS.ADMINISTRATION_BILLING_EE_PLAN_EDIT.build({
              [PARAMS.PLAN_ID]: planId,
            }),
          ],
        ]}
        allCentered
        hideChildrenOnLoading={false}
      >
        <Typography variant="h5">{t('administration_ee_plan_edit')}</Typography>
        <SelfHostedEePlanEditForm planId={planId} />
      </BaseAdministrationView>
    </DashboardPage>
  );
};
