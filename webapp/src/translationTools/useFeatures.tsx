import { useTranslate } from '@tolgee/react';
import { FeatureLink } from 'tg.component/billing/FeatureLink';
import { components } from 'tg.service/apiSchema.generated';

type Feature =
  components['schemas']['SelfHostedEePlanModel']['enabledFeatures'][number];

export function useFeatures() {
  const { t } = useTranslate();
  return {
    WEBHOOKS: t('billing_subscriptions_webhooks'),
    STANDARD_SUPPORT: t('billing_subscriptions_standard_support'),
    PROJECT_LEVEL_CONTENT_STORAGES: t(
      'billing_subscriptions_project_level_content_storages'
    ),
    AI_PROMPT_CUSTOMIZATION: t('billing_subscriptions_ai_prompt_customization'),
    GRANULAR_PERMISSIONS: t(
      'billing_subscriptions_granular_permissions_feature'
    ),
    MULTIPLE_CONTENT_DELIVERY_CONFIGS: t(
      'billing_subscriptions_multiple_content_delivery_configs'
    ),
    ACCOUNT_MANAGER: t('billing_subscriptions_account_manager_feature'),
    PREMIUM_SUPPORT: t('billing_subscriptions_premium_support_feature'),
    DEDICATED_SLACK_CHANNEL: t('billing_subscriptions_dedicated_slack_channel'),
    DEPLOYMENT_ASSISTANCE: t('billing_subscriptions_deployment_assistance'),
    ASSISTED_UPDATES: t('billing_subscriptions_assisted_updates_feature'),
    BACKUP_CONFIGURATION: t(
      'billing_subscriptions_backup_configuration_feature'
    ),
    TEAM_TRAINING: t('billing_subscriptions_team_training'),
    PRIORITIZED_FEATURE_REQUESTS: t(
      'billing_subscriptions_prioritized_feature_requests'
    ),
    SLACK_INTEGRATION: t('billing_subscriptions_slack_integration'),
    TASKS: t('billing_subscriptions_tasks'),
  } as const satisfies Record<Feature, string>;
}

export const useFeatureLabel = () => {
  const features = useFeatures();
  return function Feature(featureType: Feature) {
    const translation = features[featureType];

    if (!translation) {
      return featureType;
    }

    switch (featureType) {
      case 'GRANULAR_PERMISSIONS':
        return (
          <FeatureLink
            newTab
            href="https://tolgee.io/platform/projects_and_organizations/members#granular-permissions"
          >
            {translation}
          </FeatureLink>
        );
      default:
        return translation;
    }
  };
};
