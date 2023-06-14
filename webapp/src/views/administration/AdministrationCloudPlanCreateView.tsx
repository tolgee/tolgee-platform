import { Box, Typography } from '@mui/material';
import { useTranslate, T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { BaseAdministrationView } from './components/BaseAdministrationView';
import { CloudPlanForm } from './components/CloudPlanForm';

export const AdministrationCloudPlanCreateView = () => {
  const messaging = useMessage();
  const history = useHistory();
  const { t } = useTranslate();

  const createPlanLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/cloud-plans',
    method: 'post',
  });

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
        containerMaxWidth="lg"
        allCentered
        hideChildrenOnLoading={false}
      >
        <Box>
          <Typography variant="h5">
            {t('administration_cloud_plan_create')}
          </Typography>
          <CloudPlanForm
            loading={createPlanLoadable.isLoading}
            onSubmit={(values) => {
              createPlanLoadable.mutate(
                {
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
                      <T keyName="administration_cloud_plan_created_success" />
                    );
                    history.push(
                      LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build()
                    );
                  },
                }
              );
            }}
            initialData={{
              type: 'PAY_AS_YOU_GO',
              name: '',
              stripeProductId: undefined,
              prices: {
                perSeat: 0,
                subscriptionMonthly: 0,
                subscriptionYearly: 0,
              },
              includedUsage: {
                seats: 0,
                translations: 0,
                mtCredits: 0,
              },
              enabledFeatures: [],
              public: true,
              forOrganizationIds: [],
            }}
          />
        </Box>
      </BaseAdministrationView>
    </DashboardPage>
  );
};
