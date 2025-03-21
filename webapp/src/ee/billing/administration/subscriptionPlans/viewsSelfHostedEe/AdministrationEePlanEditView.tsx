import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useHistory, useRouteMatch } from 'react-router-dom';

import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { SelfHostedEePlanForm } from '../components/planForm/selfHostedEe/SelfHostedEePlanForm';
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
