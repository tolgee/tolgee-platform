import { Box, Button, Chip, styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { ArrowRight, Settings01 } from '@untitled-ui/icons-react';
import { PricePrimary } from 'tg.ee.module/billing/component/Price/PricePrimary';
import React, { useState } from 'react';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { FullWidthTooltip } from 'tg.component/common/FullWidthTooltip';
import { LINKS } from 'tg.constants/links';
import { Link } from 'react-router-dom';
import clsx from 'clsx';
import { PlanType } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/types';

const TooltipText = styled('div')`
  white-space: nowrap;
`;

const TooltipTitle = styled('div')`
  font-weight: bold;
  font-size: 14px;
  line-height: 17px;
`;

const MigrationDetailBox = styled(Box)`
  &.inactive {
    opacity: 0.5;
  }
`;

export const PlanMigratingChip = ({
  migrationId,
  isEnabled,
  planType = 'cloud',
}: {
  migrationId?: number;
  isEnabled?: boolean;
  planType?: PlanType;
}) => {
  if (!migrationId) {
    return null;
  }
  const [opened, setOpened] = useState(false);
  const infoCloudLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans/migration/{migrationId}',
    method: 'get',
    path: { migrationId: migrationId },
    options: {
      enabled: planType == 'cloud' && !!migrationId && opened,
    },
  });

  const infoSelfHostedEeLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}',
    method: 'get',
    path: { migrationId: migrationId },
    options: {
      enabled: planType == 'self-hosted' && !!migrationId && opened,
    },
  });

  const loadable =
    planType == 'cloud' ? infoCloudLoadable : infoSelfHostedEeLoadable;

  const info = loadable.data;

  const configureLink =
    planType == 'cloud'
      ? LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_MIGRATION_EDIT
      : LINKS.ADMINISTRATION_BILLING_EE_PLAN_MIGRATION_EDIT;

  const { t } = useTranslate();
  return (
    <FullWidthTooltip
      sx={{ maxWidth: 'initial' }}
      onOpen={() => setOpened(true)}
      title={
        loadable.isLoading ? (
          <Box padding={3}>
            <SpinnerProgress size={24} />
          </Box>
        ) : info ? (
          <Box p={1}>
            <Box display="flex" gap={1}>
              <Typography variant={'h6'} mb={2}>
                {t('administration_plan_migration_details')}
              </Typography>
              {isEnabled ? (
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
            <MigrationDetailBox className={clsx(!isEnabled && 'inactive')}>
              <Box display="flex" gap={1} alignItems="center" mb={1}>
                <Chip
                  sx={{
                    padding: '10px',
                    height: 'auto',
                    '& .MuiChip-label': {
                      display: 'block',
                      whiteSpace: 'normal',
                    },
                  }}
                  label={
                    <Box>
                      <Typography variant="caption">
                        {t('administration_plan_migration_from')}
                      </Typography>
                      <TooltipTitle>{info?.sourcePlan.name}</TooltipTitle>
                      {info?.targetPlan.prices && (
                        <TooltipText>
                          <PricePrimary
                            prices={info?.sourcePlan.prices}
                            period={'YEARLY'}
                            highlightColor={''}
                            sx={{
                              fontSize: 14,
                              fontWeight: 500,
                            }}
                            noPeriodSwitch={true}
                          />
                        </TooltipText>
                      )}
                    </Box>
                  }
                />
                <ArrowRight width={18} height={18} />
                <Chip
                  sx={{
                    padding: '10px',
                    height: 'auto',
                    '& .MuiChip-label': {
                      display: 'block',
                      whiteSpace: 'normal',
                    },
                  }}
                  label={
                    <Box>
                      <Typography variant="caption">
                        {t('administration_plan_migration_to')}
                      </Typography>
                      <TooltipTitle>{info?.targetPlan.name}</TooltipTitle>
                      {info?.targetPlan.prices && (
                        <TooltipText>
                          <PricePrimary
                            prices={info?.targetPlan.prices}
                            period={'YEARLY'}
                            highlightColor={''}
                            sx={{
                              fontSize: 14,
                              fontWeight: 500,
                            }}
                            noPeriodSwitch={true}
                          />
                        </TooltipText>
                      )}
                    </Box>
                  }
                />
              </Box>
              <Box mt={2}>
                <Typography variant={'subtitle2'}>
                  {t('administration_plan_migration_timing')}
                </Typography>
                <Box>
                  <TooltipText>
                    <T
                      keyName="administration_plan_migration_monthly_timing"
                      params={{ days: info?.monthlyOffsetDays, b: <b /> }}
                    />
                  </TooltipText>
                  <TooltipText>
                    <T
                      keyName="administration_plan_migration_yearly_timing"
                      params={{ days: info?.yearlyOffsetDays, b: <b /> }}
                    />
                  </TooltipText>
                </Box>
              </Box>
            </MigrationDetailBox>
            <Box display="flex" justifyContent="center" mt={3}>
              <Button
                key="create-migration"
                variant="contained"
                size="medium"
                startIcon={<Settings01 width={19} height={19} />}
                component={Link}
                color="warning"
                to={configureLink.build({
                  migrationId: info.id,
                })}
                data-cy="administration-cloud-plans-create-migration"
              >
                {t('global_configure')}
              </Button>
            </Box>
          </Box>
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
        data-cy="administration-cloud-plans-item-is-migrating-badge"
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
