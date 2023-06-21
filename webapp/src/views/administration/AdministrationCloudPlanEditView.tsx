import { Box, CircularProgress, Typography } from '@mui/material';
import { useTranslate, T } from '@tolgee/react';
import { useHistory, useRouteMatch } from 'react-router-dom';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { BaseAdministrationView } from './components/BaseAdministrationView';
import { CloudPlanForm } from './components/CloudPlanForm';

export const AdministrationCloudPlanEditView = () => {
  const match = useRouteMatch();
  const { t } = useTranslate();
  const messaging = useMessage();
  const history = useHistory();

  const planId = match.params[PARAMS.PLAN_ID];

  const planLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans/{planId}',
    method: 'get',
    path: { planId },
  });

  const planEditLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/cloud-plans/{planId}',
    method: 'put',
    invalidatePrefix: '/v2/administration/billing/cloud-plans',
  });

  if (planLoadable.isLoading) {
    return <CircularProgress />;
  }

  const planData = planLoadable.data;

  if (!planData) {
    return null;
  }

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
        containerMaxWidth="lg"
        allCentered
        hideChildrenOnLoading={false}
      >
        <Box>
          <Typography variant="h5">
            {t('administration_cloud_plan_edit')}
          </Typography>
          <CloudPlanForm
            planId={planId}
            loading={planEditLoadable.isLoading}
            initialData={{
              ...planData,
              includedUsage: {
                seats: planData.includedUsage.seats,
                mtCredits: planData.includedUsage.mtCredits,
                translations:
                  planData.type === 'SLOTS_FIXED'
                    ? planData.includedUsage.translationSlots
                    : planData.includedUsage.translations,
              },
            }}
            onSubmit={(values) => {
              planEditLoadable.mutate(
                {
                  path: { planId },
                  content: {
                    'application/json': {
                      ...values,
                      stripeProductId: values.stripeProductId!,
                      free: false,
                      forOrganizationIds: values.public
                        ? []
                        : values.forOrganizationIds,
                    },
                  },
                },
                {
                  onSuccess() {
                    messaging.success(
                      <T keyName="administration_cloud_plan_updated_success" />
                    );
                    history.push(
                      LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build()
                    );
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
