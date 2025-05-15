import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type Feature =
  components['schemas']['SelfHostedEePlanModel']['enabledFeatures'][number];

export function useFeatures() {
  const { t } = useTranslate();
  return {
    GRANULAR_PERMISSIONS: t(
      'billing_subscriptions_granular_permissions_feature'
    ),
    AI_PROMPT_CUSTOMIZATION: t('billing_subscriptions_ai_prompt_customization'),
    TASKS: t('billing_subscriptions_tasks'),
    WEBHOOKS: t('billing_subscriptions_webhooks'),
    SLACK_INTEGRATION: t('billing_subscriptions_slack_integration'),
    SSO: t('billing_subscriptions_sso'),
    PREMIUM_SUPPORT: t('billing_subscriptions_premium_support_feature'),
    STANDARD_SUPPORT: t('billing_subscriptions_standard_support'),
    ORDER_TRANSLATION: t('billing_subscriptions_ordering_translation'),
    MULTIPLE_CONTENT_DELIVERY_CONFIGS: t(
      'billing_subscriptions_multiple_content_delivery_configs'
    ),
    PROJECT_LEVEL_CONTENT_STORAGES: t(
      'billing_subscriptions_project_level_content_storages'
    ),
    GLOSSARY: t('billing_subscriptions_glossary'),

    ACCOUNT_MANAGER: t('billing_subscriptions_account_manager_feature'),
    DEDICATED_SLACK_CHANNEL: t('billing_subscriptions_dedicated_slack_channel'),
    DEPLOYMENT_ASSISTANCE: t('billing_subscriptions_deployment_assistance'),
    PRIORITIZED_FEATURE_REQUESTS: t(
      'billing_subscriptions_prioritized_feature_requests'
    ),
    ASSISTED_UPDATES: t('billing_subscriptions_assisted_updates_feature'),
    BACKUP_CONFIGURATION: t(
      'billing_subscriptions_backup_configuration_feature'
    ),
    TEAM_TRAINING: t('billing_subscriptions_team_training'),
  } as const satisfies Record<Feature, string>;
}
