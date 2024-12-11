import { Link } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  Chip,
  IconButton,
  ListItem,
  ListItemText,
  Paper,
} from '@mui/material';
import { X } from '@untitled-ui/icons-react';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS, PARAMS } from 'tg.constants/links';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { components } from 'tg.service/billingApiSchema.generated';

type SelfHostedEePlanModel = components['schemas']['SelfHostedEePlanModel'];

export const AdministrationEePlansView = () => {
  const messaging = useMessage();
  const { t } = useTranslate();

  const plansLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans',
    method: 'get',
  });

  const deletePlanLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/{planId}',
    method: 'delete',
    invalidatePrefix: '/v2/administration/billing/self-hosted-ee-plans',
  });

  function deletePlan(plan: SelfHostedEePlanModel) {
    confirmation({
      hardModeText: plan.name,
      message: <T keyName="administration_ee_plan_delete_message" />,
      onConfirm: () =>
        deletePlanLoadable.mutate(
          {
            path: { planId: plan.id },
          },
          {
            onSuccess() {
              messaging.success(
                <T keyName="administration_ee_plan_deleted_success" />
              );
            },
          }
        ),
    });
  }

  return (
    <DashboardPage>
      <BaseAdministrationView
        title={t('administration_ee_plans')}
        windowTitle={t('administration_ee_plans')}
        navigation={[
          [
            t('administration_ee_plans'),
            LINKS.ADMINISTRATION_BILLING_EE_PLANS.build(),
          ],
        ]}
        allCentered
        hideChildrenOnLoading={false}
        addLinkTo={LINKS.ADMINISTRATION_BILLING_EE_PLAN_CREATE.build()}
        onAdd={() => {}}
      >
        <Paper variant="outlined">
          {plansLoadable.data?._embedded?.plans?.map((plan, i) => (
            <ListItem key={i} data-cy="administration-ee-plans-item">
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
                      data-cy="administration-ee-plans-item-public-badge"
                      size="small"
                      label={t('administration_ee_plan_public_badge')}
                    />
                  )}
                </Box>
                <Box>
                  <Button
                    size="small"
                    component={Link}
                    to={LINKS.ADMINISTRATION_BILLING_EE_PLAN_EDIT.build({
                      [PARAMS.PLAN_ID]: plan.id,
                    })}
                    data-cy="administration-ee-plans-item-edit"
                  >
                    {t('administration_ee_plan_edit_button')}
                  </Button>
                  <IconButton
                    size="small"
                    onClick={() => deletePlan(plan)}
                    data-cy="administration-ee-plans-item-delete"
                  >
                    <X />
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
