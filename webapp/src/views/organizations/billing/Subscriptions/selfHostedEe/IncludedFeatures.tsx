import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { T, useTranslate } from '@tolgee/react';
import { Box, Typography, styled } from '@mui/material';
import { PlanFeature } from './PlanFeature';

const StyledListWrapper = styled(Box)`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 300px));
  margin-top: 8px;
  gap: 16px;
`;

export const IncludedFeatures: FC<{
  features: components['schemas']['SelfHostedEePlanModel']['enabledFeatures'];
}> = ({ features }) => {
  const { t } = useTranslate();

  const featuresMap: Record<
    components['schemas']['SelfHostedEePlanModel']['enabledFeatures'][0],
    string
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
      <Typography
        mt={1}
        mb={2}
        sx={{ fontSize: '12px', fontStyle: 'italic' }}
        color="primary"
      >
        <T keyName="billing_subscriptions_plan_includes_title" />
      </Typography>
      <StyledListWrapper>
        {features.map((feature) => (
          <PlanFeature key={feature} name={featuresMap[feature]} />
        ))}
      </StyledListWrapper>
    </Box>
  );
};
