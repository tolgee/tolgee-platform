import { T, useTranslate } from '@tolgee/react';
import { assertUnreachable } from 'tg.fixtures/assertUnreachable';
import { components } from 'tg.service/apiSchema.generated';
import { Announcement } from './Announcement';
import { BannerLink } from './BannerLink';

type AnnouncementDtoType = components['schemas']['AnnouncementDto']['type'];

export function useAnnouncement() {
  const { t } = useTranslate();
  return function AnnouncementWrapper(value: AnnouncementDtoType) {
    switch (value) {
      case 'FEATURE_BATCH_OPERATIONS':
        return (
          <Announcement
            content={t('announcement_feature_batch_operations')}
            link="https://tolgee.io/platform/translation_keys/batch_operations"
          />
        );
      case 'FEATURE_MT_FORMALITY':
        return (
          <Announcement
            content={t('announcement_feature_mt_formality')}
            link="https://tolgee.io/platform/projects_and_organizations/languages#machine-translation-settings"
          />
        );
      case 'FEATURE_CONTENT_DELIVERY_AND_WEBHOOKS':
        return (
          <Announcement
            content={
              <T
                keyName="announcement_feature_content_delivery_and_webhooks"
                params={{
                  'link-delivery': (
                    <BannerLink href="https://tolgee.io/platform/projects_and_organizations/content_delivery" />
                  ),
                  'link-webhooks': (
                    <BannerLink href="https://tolgee.io/platform/projects_and_organizations/webhooks" />
                  ),
                }}
              />
            }
          />
        );
      case 'NEW_PRICING':
        return (
          <Announcement
            content={<T keyName="announcement_new_pricing" />}
            link="https://tolgee.io/blog/new-pricing-2024-01"
          />
        );

      case 'FEATURE_AI_CUSTOMIZATION':
        return (
          <Announcement
            content={<T keyName="announcement_feature_ai_customization" />}
            link="https://tolgee.io/blog/releasing-ai-customizations"
          />
        );

      case 'FEATURE_VISUAL_EDITOR':
        return (
          <Announcement
            content={
              <T keyName="announcement_visual_editor_and_formats_support" />
            }
            link="https://tolgee.io/blog/releasing-visual-editor-and-formats-support"
          />
        );
      case 'FEATURE_CLI_2':
        return (
          <Announcement
            content={<T keyName="announcement_cli_2" />}
            link="https://tolgee.io/blog/cli-2-features"
          />
        );

      case 'FEATURE_TASKS':
        return (
          <Announcement
            content={<T keyName="announcement_feature_tasks" />}
            link="https://tolgee.io/blog/new-feature-tasks"
          />
        );

      default:
        assertUnreachable(value);
    }
  };
}
