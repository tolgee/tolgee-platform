import { useTranslate } from '@tolgee/react';
import { assertUnreachable } from 'tg.fixtures/assertUnreachable';
import { components } from 'tg.service/apiSchema.generated';
import { Announcement } from './Announcement';

type AnnouncementDtoType = components['schemas']['AnnouncementDto']['type'];

export function useAnnouncement() {
  const { t } = useTranslate();

  return (value: AnnouncementDtoType) => {
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
      default:
        assertUnreachable(value);
    }
  };
}
