import { Box, Button, Chip, styled, Typography } from '@mui/material';
import clsx from 'clsx';
import { ArrowRight, Settings01 } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import React from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { Link } from 'tg.constants/links';
import { Link as RouterLink } from 'react-router-dom';
import { PlanMigrationPlanPriceDetail } from 'tg.ee.module/billing/component/Plan/migration/PlanMigrationPlanPriceDetail';

type CloudPlanModel = components['schemas']['CloudPlanMigrationModel'];
type SelfHostedEePlanAdministrationModel =
  components['schemas']['AdministrationSelfHostedEePlanMigrationModel'];

const TooltipText = styled('div')`
  white-space: nowrap;
`;

const MigrationDetailBox = styled(Box)`
  &.inactive {
    opacity: 0.5;
  }
`;

type Props = {
  migration: CloudPlanModel | SelfHostedEePlanAdministrationModel;
  editLink: Link;
};

export const PlanMigrationDetail = ({ migration, editLink }: Props) => {
  const { t } = useTranslate();
  if (!migration) {
    return (
      <Box padding={3}>
        <Typography variant={'subtitle2'}>
          {t('administration_plan_migration_not_found')}
        </Typography>
      </Box>
    );
  }
  return (
    <Box p={1} data-cy="plan-migration-tooltip-detail">
      <Box display="flex" gap={1}>
        <Typography variant={'h6'} mb={2}>
          {t('administration_plan_migration_details')}
        </Typography>
        {migration.enabled ? (
          <Chip
            color="primary"
            label={t('administration_plan_migration_active')}
          />
        ) : (
          <Chip
            color="default"
            label={t('administration_plan_migration_pending')}
          />
        )}
      </Box>
      <MigrationDetailBox className={clsx(!migration.enabled && 'inactive')}>
        <Box display="flex" gap={1} alignItems="center" mb={1}>
          <PlanMigrationPlanPriceDetail plan={migration.sourcePlan} />
          <ArrowRight width={18} height={18} />
          <PlanMigrationPlanPriceDetail plan={migration.targetPlan} />
        </Box>
        <Box mt={2}>
          <Typography variant={'subtitle2'}>
            {t('administration_plan_migration_timing')}
          </Typography>
          <Box>
            <TooltipText>
              <T
                keyName="administration_plan_migration_monthly_timing"
                params={{ days: migration.monthlyOffsetDays, b: <b /> }}
              />
            </TooltipText>
            <TooltipText>
              <T
                keyName="administration_plan_migration_yearly_timing"
                params={{ days: migration.yearlyOffsetDays, b: <b /> }}
              />
            </TooltipText>
          </Box>
        </Box>
      </MigrationDetailBox>
      <Box display="flex" justifyContent="center" mt={3}>
        <Button
          key="edit-migration"
          variant="contained"
          size="medium"
          startIcon={<Settings01 width={19} height={19} />}
          component={RouterLink}
          color="warning"
          to={editLink.build({
            migrationId: migration.id,
          })}
          data-cy="administration-plans-edit-migration"
        >
          {t('global_configure')}
        </Button>
      </Box>
    </Box>
  );
};
