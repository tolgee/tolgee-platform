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
  styled,
} from '@mui/material';
import { Settings01, X } from '@untitled-ui/icons-react';

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
import { PlanSubscriptionCount } from 'tg.ee.module/billing/component/Plan/PlanSubscriptionCount';
import { PlanListPriceInfo } from 'tg.ee.module/billing/component/Plan/PlanListPriceInfo';
import { PlanArchivedChip } from 'tg.ee.module/billing/component/Plan/PlanArchivedChip';
import clsx from 'clsx';
import { PlanMigratingChip } from 'tg.ee.module/billing/component/Plan/PlanMigratingChip';

type SelfHostedEePlanAdministrationModel =
  components['schemas']['SelfHostedEePlanAdministrationModel'];

const StyledListItemText = styled(ListItemText)(({ theme }) => ({
  '&.archived': {
    color: theme.palette.text.disabled,
  },
}));

export const AdministrationEePlansView = () => {
  const messaging = useMessage();
  const { t } = useTranslate();

  const plansLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans',
    method: 'get',
    query: {},
  });

  const archivePlanLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/{planId}/archive',
    method: 'put',
    invalidatePrefix: '/v2/administration/billing/self-hosted-ee-plans',
  });

  const deletePlanLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/self-hosted-ee-plans/{planId}',
    method: 'delete',
    invalidatePrefix: '/v2/administration/billing/self-hosted-ee-plans',
  });

  function archivePlan(plan: SelfHostedEePlanAdministrationModel) {
    confirmation({
      message: <T keyName="administration_plan_archive_message" />,
      confirmButtonText: <T keyName="general_archive" />,
      onConfirm: () =>
        archivePlanLoadable.mutate(
          {
            path: { planId: plan.id },
          },
          {
            onSuccess() {
              messaging.success(
                <T keyName="administration_plan_archived_success" />
              );
            },
          }
        ),
    });
  }

  function deletePlan(plan: SelfHostedEePlanAdministrationModel) {
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
        customButtons={[
          <Button
            key="create-migration"
            variant="contained"
            size="medium"
            startIcon={<Settings01 width={19} height={19} />}
            component={Link}
            color="warning"
            to={LINKS.ADMINISTRATION_BILLING_EE_PLAN_MIGRATION_CREATE.build()}
            data-cy="administration-cloud-plans-create-migration"
          >
            {t('administration_cloud_plan_create_migration')}
          </Button>,
        ]}
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
                  <StyledListItemText
                    className={clsx(plan.archivedAt != null && 'archived')}
                  >
                    {plan.name}
                  </StyledListItemText>
                  <PlanArchivedChip isArchived={plan.archivedAt != null} />
                  {plan.public && (
                    <Chip
                      data-cy="administration-ee-plans-item-public-badge"
                      size="small"
                      label={t('administration_ee_plan_public_badge')}
                    />
                  )}
                  <PlanMigratingChip
                    migrationId={plan.migrationId}
                    isEnabled={plan.activeMigration}
                    planType="self-hosted"
                  />
                </Box>
                <Box display="flex" gap={2}>
                  <Box display="flex" gap={2} alignItems="center">
                    <PlanSubscriptionCount count={plan.subscriptionCount} />
                    <PlanListPriceInfo prices={plan.prices} bold />
                  </Box>
                  <Box display="flex">
                    {!plan.archivedAt && (
                      <Button
                        size="small"
                        onClick={() => archivePlan(plan)}
                        data-cy="administration-ee-plans-item-archive"
                      >
                        {t('administration_plan_archive_button')}
                      </Button>
                    )}
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
              </Box>
            </ListItem>
          ))}
        </Paper>
      </BaseAdministrationView>
    </DashboardPage>
  );
};
