import { FullWidthTooltip } from 'tg.component/common/FullWidthTooltip';
import { Box, Chip, Typography } from '@mui/material';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { PlanMigrationDetail } from 'tg.ee.module/billing/component/Plan/migration/PlanMigrationDetail';
import { T, useTranslate } from '@tolgee/react';
import React from 'react';
import { UseQueryResult } from 'react-query';
import { components } from 'tg.service/billingApiSchema.generated';
import { Link } from 'tg.constants/links';

type CloudPlanModel = components['schemas']['CloudPlanMigrationModel'];
type SelfHostedEePlanAdministrationModel =
  components['schemas']['AdministrationSelfHostedEePlanMigrationModel'];

type Props = {
  loadable: UseQueryResult<
    CloudPlanModel | SelfHostedEePlanAdministrationModel
  >;
  onOpen?: () => void;
  editLink: Link;
  isEnabled?: boolean;
};

export const PlanMigrationChip = ({
  loadable,
  onOpen,
  editLink,
  isEnabled,
}: Props) => {
  const { t } = useTranslate();
  const migration = loadable.data;
  return (
    <FullWidthTooltip
      sx={{ maxWidth: 'initial' }}
      onOpen={onOpen}
      title={
        loadable.isLoading ? (
          <Box padding={3}>
            <SpinnerProgress size={24} />
          </Box>
        ) : migration ? (
          <PlanMigrationDetail migration={migration} editLink={editLink} />
        ) : (
          <Box padding={3}>
            <Typography variant={'subtitle2'}>
              {t('administration_plan_migration_not_found')}
            </Typography>
          </Box>
        )
      }
    >
      <Chip
        data-cy="administration-plans-item-is-migrating-badge"
        size="small"
        color={isEnabled ? 'warning' : 'default'}
        sx={{ cursor: 'pointer' }}
        label={
          isEnabled ? (
            <T keyName="administration_cloud_plan_is_migrating_badge" />
          ) : (
            <T keyName="administration_cloud_plan_is_pending_badge" />
          )
        }
      />
    </FullWidthTooltip>
  );
};
