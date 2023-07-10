import { useTranslate, T } from '@tolgee/react';
import {
  ListItem,
  ListItemText,
  Paper,
  Button,
  Box,
  Chip,
  IconButton,
} from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS, PARAMS } from 'tg.constants/links';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { BaseAdministrationView } from './components/BaseAdministrationView';
import { Link } from 'react-router-dom';
import { Delete } from '@mui/icons-material';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { confirmation } from 'tg.hooks/confirmation';
import { components } from 'tg.service/billingApiSchema.generated';

type CloudPlanModel = components['schemas']['CloudPlanModel'];

export const AdministrationCloudPlansView = () => {
  const messaging = useMessage();
  const { t } = useTranslate();

  const plansLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans',
    method: 'get',
  });

  const deletePlanLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/cloud-plans/{planId}',
    method: 'delete',
    invalidatePrefix: '/v2/administration/billing/cloud-plans',
  });

  function deletePlan(plan: CloudPlanModel) {
    confirmation({
      hardModeText: plan.name,
      message: <T keyName="administration_cloud_plan_delete_message" />,
      onConfirm: () =>
        deletePlanLoadable.mutate(
          {
            path: { planId: plan.id },
          },
          {
            onSuccess() {
              messaging.success(
                <T keyName="administration_cloud_plan_deleted_success" />
              );
            },
          }
        ),
    });
  }

  useGlobalLoading(deletePlanLoadable.isLoading || plansLoadable.isFetching);

  return (
    <DashboardPage>
      <BaseAdministrationView
        title={t('administration_cloud_plans')}
        windowTitle={t('administration_cloud_plans')}
        navigation={[
          [
            t('administration_cloud_plans'),
            LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build(),
          ],
        ]}
        containerMaxWidth="lg"
        allCentered
        hideChildrenOnLoading={false}
        addLinkTo={LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_CREATE.build()}
        onAdd={() => {}}
      >
        <Paper variant="outlined">
          {plansLoadable.data?._embedded?.plans?.map((plan, i) => (
            <ListItem key={i} data-cy="administration-cloud-plans-item">
              <Box
                display="flex"
                justifyContent="space-between"
                width="100%"
                alignItems="center"
              >
                <Box display="flex" gap={2} alignItems="center">
                  <ListItemText>{plan.name}</ListItemText>
                  {plan.public && (
                    <Chip
                      data-cy="administration-cloud-plans-item-public-badge"
                      size="small"
                      label={t('administration_cloud_plan_public_badge')}
                    />
                  )}
                </Box>
                <Box>
                  <Button
                    size="small"
                    component={Link}
                    to={LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_EDIT.build({
                      [PARAMS.PLAN_ID]: plan.id,
                    })}
                    data-cy="administration-cloud-plans-item-edit"
                  >
                    {t('administration_cloud_plan_edit_button')}
                  </Button>
                  <IconButton
                    size="small"
                    onClick={() => deletePlan(plan)}
                    data-cy="administration-cloud-plans-item-delete"
                  >
                    <Delete />
                  </IconButton>
                </Box>
              </Box>
            </ListItem>
          ))}
        </Paper>
      </BaseAdministrationView>
    </DashboardPage>
  );
};
