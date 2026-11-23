import { useTranslate } from '@tolgee/react';
import { ActivityAction } from './activityCategories';

export const useActivityActionHints = (): Partial<
  Record<ActivityAction, string>
> => {
  const { t } = useTranslate();
  return {
    COMMENT_MENTION: t(
      'activity_action_comment_mention_hint',
      'Comments added or edited with an @mention of someone. These comments are also included in “Comment added” and “Comment edited”.'
    ),
    KEY_EDITED: t(
      'activity_action_key_edited_hint',
      'Any change to a key: name, tags, namespace, description or character limit, including batch changes. One edit can change several of these at once.'
    ),
    KEY_DELETED: t(
      'activity_action_key_deleted_hint',
      'Key moved to trash or permanently deleted, one by one or in batch.'
    ),
    TRANSLATION_EDITED: t(
      'activity_action_translation_edited_hint',
      'Translation text changed in the editor or via API. Clearing one translation in the editor counts here too.'
    ),
    TRANSLATION_OUTDATED: t(
      'activity_action_translation_outdated_hint',
      'Translation flagged as outdated (usually after the base text changed) or the flag removed.'
    ),
    COMMENT_RESOLVED: t(
      'activity_action_comment_resolved_hint',
      'Comment marked as resolved or opened again.'
    ),
    LABEL_ASSIGNED: t(
      'activity_action_label_assigned_hint',
      'Label added to or removed from translations, one by one or in batch.'
    ),
    TM_PROJECT_SETTINGS: t(
      'activity_action_tm_project_settings_hint',
      'Translation memory assigned to or removed from the project, or its settings for the project changed.'
    ),
    PROJECT_SETTINGS: t(
      'activity_action_project_settings_hint',
      'Project created or its settings changed: name, description, base language, namespaces, branching, ICU placeholders, suggestions, translation protection or public visibility.'
    ),
    CONTENT_DELIVERY: t(
      'activity_action_content_delivery_hint',
      'Content delivery or content storage configuration created, edited or deleted.'
    ),
    BRANCHES: t(
      'activity_action_branches_hint',
      'Branch created, renamed, deleted or merged, or its protection changed.'
    ),
    QA_ISSUES: t(
      'activity_action_qa_issues_hint',
      'QA issue on a translation ignored or un-ignored.'
    ),
  };
};

export const useActivityActionLabels = (): Record<ActivityAction, string> => {
  const { t } = useTranslate();
  return {
    KEY_CREATED: t('activity_action_key_created', 'Key created'),
    KEY_EDITED: t('activity_action_key_edited', 'Key edited'),
    KEY_DELETED: t('activity_action_key_deleted', 'Key deleted'),
    KEY_RESTORED: t('activity_action_key_restored', 'Key restored'),
    TRANSLATION_EDITED: t(
      'activity_action_translation_edited',
      'Translation edited'
    ),
    TRANSLATION_CLEARED: t(
      'activity_action_translation_cleared',
      'Translations cleared'
    ),
    TRANSLATION_COPIED: t(
      'activity_action_translation_copied',
      'Translations copied'
    ),
    TRANSLATION_OUTDATED: t(
      'activity_action_translation_outdated',
      'Marked as outdated'
    ),
    STATE_CHANGED: t('activity_action_state_changed', 'State changed'),
    MACHINE_TRANSLATED: t(
      'activity_action_machine_translated',
      'Machine translated'
    ),
    TRANSLATED_FROM_TM: t(
      'activity_action_translated_from_tm',
      'Translated from memory'
    ),
    AUTO_TRANSLATION_DISMISSED: t(
      'activity_action_auto_translation_dismissed',
      'Auto-translation flag dismissed'
    ),
    IMPORTED: t('activity_action_imported', 'Imported'),
    COMMENT_ADDED: t('activity_action_comment_added', 'Comment added'),
    COMMENT_MENTION: t(
      'activity_action_comment_mention',
      'Mention in a comment'
    ),
    COMMENT_EDITED: t('activity_action_comment_edited', 'Comment edited'),
    COMMENT_RESOLVED: t(
      'activity_action_comment_resolved',
      'Comment resolved or reopened'
    ),
    COMMENT_DELETED: t('activity_action_comment_deleted', 'Comment deleted'),
    SUGGESTION_ADDED: t('activity_action_suggestion_added', 'Suggestion added'),
    SUGGESTION_ACCEPTED: t(
      'activity_action_suggestion_accepted',
      'Suggestion accepted'
    ),
    SUGGESTION_DECLINED: t(
      'activity_action_suggestion_declined',
      'Suggestion declined'
    ),
    SUGGESTION_REACTIVATED: t(
      'activity_action_suggestion_reactivated',
      'Suggestion reactivated'
    ),
    SUGGESTION_DELETED: t(
      'activity_action_suggestion_deleted',
      'Suggestion deleted'
    ),
    SCREENSHOT_ADDED: t('activity_action_screenshot_added', 'Screenshot added'),
    SCREENSHOT_REMOVED: t(
      'activity_action_screenshot_removed',
      'Screenshot removed'
    ),
    LABEL_ASSIGNED: t(
      'activity_action_label_assigned',
      'Label assigned or removed'
    ),
    LABEL_MANAGED: t(
      'activity_action_label_managed',
      'Label created, edited or deleted'
    ),
    TASK_CREATED: t('activity_action_task_created', 'Task created'),
    TASK_UPDATED: t('activity_action_task_updated', 'Task updated'),
    TASK_FINISHED: t('activity_action_task_finished', 'Task finished'),
    TASK_CLOSED: t('activity_action_task_closed', 'Task closed'),
    TASK_REOPENED: t('activity_action_task_reopened', 'Task reopened'),
    LANGUAGE_ADDED: t('activity_action_language_added', 'Language added'),
    LANGUAGE_EDITED: t('activity_action_language_edited', 'Language edited'),
    LANGUAGE_DELETED: t('activity_action_language_deleted', 'Language deleted'),
    TM_PROJECT_SETTINGS: t(
      'activity_action_tm_project_settings',
      'Translation memory settings'
    ),
    PROJECT_SETTINGS: t('activity_action_project_settings', 'Project settings'),
    WEBHOOKS: t('activity_action_webhooks', 'Webhooks'),
    CONTENT_DELIVERY: t(
      'activity_action_content_delivery',
      'Content delivery and storage'
    ),
    AI_PROMPTS: t('activity_action_ai_prompts', 'AI prompts'),
    BRANCHES: t('activity_action_branches', 'Branches'),
    QA_ISSUES: t('activity_action_qa_issues', 'QA issues ignored'),
  };
};
