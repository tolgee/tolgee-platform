import { useTranslate } from '@tolgee/react';
import { ActivityCategory } from './activityCategories';

export const useActivityCategoryLabels = (): Record<
  ActivityCategory,
  string
> => {
  const { t } = useTranslate();
  return {
    KEYS: t('activity_stream_category_keys', 'Keys'),
    TRANSLATIONS: t('activity_stream_category_translations', 'Translations'),
    REVIEWS: t('activity_stream_category_reviews', 'Reviews and states'),
    MACHINE_TRANSLATION: t(
      'activity_stream_category_mt',
      'Machine translation'
    ),
    IMPORTS: t('activity_stream_category_imports', 'Imports'),
    COMMENTS: t('activity_stream_category_comments', 'Comments'),
    SUGGESTIONS: t('activity_stream_category_suggestions', 'Suggestions'),
    SCREENSHOTS: t('activity_stream_category_screenshots', 'Screenshots'),
    LABELS: t('activity_stream_category_labels', 'Labels'),
    TASKS: t('activity_stream_category_tasks', 'Tasks'),
    LANGUAGES: t('activity_stream_category_languages', 'Languages'),
    TRANSLATION_MEMORY: t(
      'activity_category_translation_memory',
      'Translation memory'
    ),
    PROJECT: t(
      'activity_stream_category_project',
      'Project settings and integrations'
    ),
  };
};
