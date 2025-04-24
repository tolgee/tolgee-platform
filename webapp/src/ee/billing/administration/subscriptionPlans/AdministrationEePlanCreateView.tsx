import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { EePlanForm } from './components/planForm/EePlanForm';

export const AdministrationEePlanCreateView = () => {
  const messaging = useMessage();
  const history = useHistory();
  const { t } = useTranslate();

  const createPlanLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans',
    method: 'post',
  });

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
        <Box>
          <Typography variant="h5">
            {t('administration_ee_plan_create')}
          </Typography>
          <EePlanForm
            loading={createPlanLoadable.isLoading}
            onSubmit={(values) => {
              createPlanLoadable.mutate(
                {
                  content: {
                    'application/json': {
                      ...values,
                      stripeProductId: values.stripeProductId,
                      forOrganizationIds: values.public
                        ? []
                        : values.forOrganizationIds,
                    },
                  },
                },
                {
                  onSuccess() {
                    messaging.success(
                      <T keyName="administration_ee_plan_created_success" />
                    );
                    history.push(LINKS.ADMINISTRATION_BILLING_EE_PLANS.build());
                  },
                }
              );
            }}
            initialData={{
              name: '',
              stripeProductId: undefined,
              prices: {
                perSeat: 0,
                subscriptionMonthly: 0,
                subscriptionYearly: 0,
                perThousandMtCredits: 0,
              },
              includedUsage: {
                seats: 0,
                translations: 0,
                mtCredits: 0,
                keys: 0,
              },
              forOrganizationIds: [],
              enabledFeatures: [],
              public: true,
              free: false,
              nonCommercial: false,
            }}
          />
        </Box>
      </BaseAdministrationView>
    </DashboardPage>
  );
};
