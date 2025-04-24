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
import { EePlanForm } from './components/planForm/EePlanForm';

export const AdministrationEePlanEditView = () => {
  const match = useRouteMatch();
  const { t } = useTranslate();
  const messaging = useMessage();
  const history = useHistory();

  const planId = match.params[PARAMS.PLAN_ID];

  const planLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans/{planId}',
    method: 'get',
    path: { planId },
  });

  const planEditLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/{planId}',
    method: 'put',
    invalidatePrefix: '/v2/administration/billing/self-hosted-ee-plans',
  });

  if (planLoadable.isLoading) {
    return <SpinnerProgress />;
  }

  const planData = planLoadable.data;

  if (!planData) {
    return null;
  }

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
        <Box>
          <Typography variant="h5">
            {t('administration_ee_plan_edit')}
          </Typography>
          <EePlanForm
            planId={planId}
            loading={false}
            initialData={{
              ...planData,
              includedUsage: {
                seats: planData.includedUsage.seats,
                mtCredits: planData.includedUsage.mtCredits,
                translations: planData.includedUsage.translations,
              },
            }}
            onSubmit={(values) => {
              planEditLoadable.mutate(
                {
                  path: { planId },
                  content: {
                    'application/json': {
                      ...values,
                      metricType: 'KEYS_SEATS',
                      stripeProductId: values.stripeProductId!,
                      forOrganizationIds: values.public
                        ? []
                        : values.forOrganizationIds,
                    },
                  },
                },
                {
                  onSuccess() {
                    messaging.success(
                      <T keyName="administration_ee_plan_updated_success" />
                    );
                    history.push(LINKS.ADMINISTRATION_BILLING_EE_PLANS.build());
                  },
                }
              );
            }}
          />
        </Box>
      </BaseAdministrationView>
    </DashboardPage>
  );
};
