import { FC, ReactNode } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { T, useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';

export const IncludedFeatures: FC<{
  features: components['schemas']['SelfHostedEePlanModel']['enabledFeatures'];
}> = ({ features }) => {
  const { t } = useTranslate();

  const featuresMap: Record<
    components['schemas']['SelfHostedEePlanModel']['enabledFeatures'][0],
    ReactNode
  > = {
    GRANULAR_PERMISSIONS: t(
      'billing_subscriptions_granular_permissions_feature'
    ),
    PREMIUM_SUPPORT: t('billing_subscriptions_premium_support_feature'),
    BACKUP_CONFIGURATION: t(
      'billing_subscriptions_backup_configuration_feature'
    ),
    ACCOUNT_MANAGER: t('billing_subscriptions_account_manager_feature'),
    ASSISTED_UPDATES: t('billing_subscriptions_assisted_updates_feature'),
    DEDICATED_SLACK_CHANNEL: t('billing_subscriptions_dedicated_slack_channel'),
    DEPLOYMENT_ASSISTANCE: t('billing_subscriptions_deployment_assistance'),
    PRIORITIZED_FEATURE_REQUESTS: t(
      'billing_subscriptions_prioritized_feature_requests'
    ),
    TEAM_TRAINING: t('billing_subscriptions_team_training'),
  };

  return (
    <Box>
      <Box mt={1} sx={{ fontSize: '12px', fontStyle: 'italic' }}>
        <T keyName="billing_subscriptions_plan_includes_title" />
      </Box>
      <StyledListWrapper>
        {features.map((feature) => (
          <StyledListItem key={feature}>{featuresMap[feature]}</StyledListItem>
        ))}
      </StyledListWrapper>
    </Box>
  );
};

const StyledListWrapper = styled(Box)`
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  margin-top: 8px;
  gap: 16px;
`;

const StyledListItem = styled(Box)`
  :before {
    content: '✓';
    padding-right: 4px;
  }
`;
