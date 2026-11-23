import { useTranslate } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { ProjectActivityModel } from 'tg.component/activity/types';

const PROVIDER_NAMES: Record<string, string> = {
  PROMPT: 'AI',
  GOOGLE: 'Google',
  DEEPL: 'DeepL',
  AWS: 'Amazon Translate',
  AZURE: 'Azure',
  BAIDU: 'Baidu',
};

type Entity = {
  modifications?: Record<string, { old?: unknown; new?: unknown }>;
  relations?: Record<string, { data?: Record<string, unknown> }>;
};

const entitiesOf = (data: ProjectActivityModel, entityClass: string) =>
  ((data.modifiedEntities as Record<string, Entity[]> | undefined)?.[
    entityClass
  ] ?? []) as Entity[];

const newValues = (entities: Entity[], field: string) =>
  new Set(entities.map((e) => e.modifications?.[field]?.new));

const single = <T>(values: Set<T>) =>
  values.size === 1 ? [...values][0] : undefined;

const hasKey = (data: ProjectActivityModel) =>
  JSON.stringify(data.modifiedEntities ?? {}).includes('"key"');

export const useActivityPhrase = () => {
  const { t } = useTranslate();
  const project = useProject();
  const baseTag = project.baseLanguage?.tag;

  return (data: ProjectActivityModel): string | undefined => {
    const translations = entitiesOf(data, 'Translation');
    const params = (data.params ?? {}) as Record<string, unknown>;

    switch (data.type) {
      case 'CREATE_KEY':
        return t('activity_phrase_key_created', 'created key');
      case 'KEY_NAME_EDIT':
        return t('activity_phrase_key_renamed', 'renamed key');
      case 'KEY_TAGS_EDIT':
      case 'BATCH_TAG_KEYS':
      case 'BATCH_UNTAG_KEYS':
      case 'COMPLEX_TAG_OPERATION':
        return t('activity_phrase_key_tags', 'changed tags of');
      case 'NAMESPACE_EDIT':
      case 'BATCH_SET_KEYS_NAMESPACE':
        return t('activity_phrase_key_namespace', 'changed namespace of');
      case 'COMPLEX_EDIT':
      case 'KEY_CHARACTER_LIMIT_EDIT':
        return t('activity_phrase_key_changed', 'changed key');
      case 'KEY_SOFT_DELETE':
      case 'KEY_HARD_DELETE':
      case 'BATCH_KEY_HARD_DELETE':
        return t('activity_phrase_key_deleted', 'deleted key');
      case 'KEY_RESTORE':
      case 'BATCH_KEY_RESTORE':
        return t('activity_phrase_key_restored', 'restored key');

      case 'SET_TRANSLATIONS': {
        const isBase = translations.some(
          (e) =>
            e.relations?.language?.data?.tag === baseTag &&
            e.modifications?.text
        );
        const texts = translations
          .map((e) => e.modifications?.text)
          .filter(Boolean) as { old?: unknown; new?: unknown }[];
        if (texts.length && texts.every((m) => !m.new)) {
          return t(
            'activity_phrase_translation_deleted',
            'deleted translation of'
          );
        }
        if (isBase) {
          return t('activity_phrase_base_changed', 'changed base text of');
        }
        if (texts.length && texts.every((m) => !m.old)) {
          return t('activity_phrase_translated', 'translated');
        }
        return t(
          'activity_phrase_translation_changed',
          'changed translation of'
        );
      }
      case 'BATCH_CLEAR_TRANSLATIONS':
        return t('activity_phrase_cleared', 'cleared translation of');
      case 'BATCH_COPY_TRANSLATIONS':
        return t('activity_phrase_copied', 'copied translation of');
      case 'SET_OUTDATED_FLAG':
        return single(newValues(translations, 'outdated')) === false
          ? t(
              'activity_phrase_outdated_removed',
              'removed the outdated flag from'
            )
          : t('activity_phrase_outdated', 'marked as outdated');

      case 'SET_TRANSLATION_STATE':
      case 'BATCH_SET_TRANSLATION_STATE': {
        const state =
          data.type === 'BATCH_SET_TRANSLATION_STATE'
            ? params.state
            : single(newValues(translations, 'state'));
        if (state === 'REVIEWED') {
          return t('activity_phrase_reviewed', 'reviewed');
        }
        if (state === 'TRANSLATED') {
          return t('activity_phrase_unreviewed', 'unreviewed');
        }
        if (state === 'UNTRANSLATED') {
          return t('activity_phrase_untranslated', 'marked as untranslated');
        }
        return t('activity_phrase_state_changed', 'changed the state of');
      }

      case 'AUTO_TRANSLATE':
      case 'BATCH_MACHINE_TRANSLATE': {
        const provider = single(newValues(translations, 'mtProvider')) as
          | string
          | undefined;
        if (provider) {
          return t(
            'activity_phrase_translated_with',
            'translated using {provider}',
            {
              provider: PROVIDER_NAMES[provider] ?? provider,
            }
          );
        }
        return t('activity_phrase_machine_translated', 'machine-translated');
      }
      case 'BATCH_PRE_TRANSLATE_BY_TM':
        return t('activity_phrase_pretranslated_tm', 'pre-translated using TM');
      case 'DISMISS_AUTO_TRANSLATED_STATE':
        return t(
          'activity_phrase_accepted_mt',
          'accepted machine translation of'
        );

      case 'IMPORT':
        return t('activity_phrase_imported', 'imported');

      case 'TRANSLATION_COMMENT_ADD':
        return t(
          'activity_phrase_commented',
          'commented on the translation of'
        );
      case 'TRANSLATION_COMMENT_EDIT':
        return t('activity_phrase_comment_edited', 'edited a comment on');
      case 'TRANSLATION_COMMENT_DELETE':
        return t('activity_phrase_comment_deleted', 'deleted a comment on');
      case 'TRANSLATION_COMMENT_SET_STATE':
        return single(
          newValues(entitiesOf(data, 'TranslationComment'), 'state')
        ) === 'RESOLVED'
          ? t('activity_phrase_comment_resolved', 'resolved a comment on')
          : t('activity_phrase_comment_reopened', 'reopened a comment on');

      case 'CREATE_SUGGESTION':
        return t('activity_phrase_suggested', 'suggested a translation of');
      case 'ACCEPT_SUGGESTION':
        return t(
          'activity_phrase_suggestion_accepted',
          'accepted a suggestion for'
        );
      case 'DECLINE_SUGGESTION':
        return t(
          'activity_phrase_suggestion_declined',
          'declined a suggestion for'
        );
      case 'SUGGESTION_SET_ACTIVE':
        return t(
          'activity_phrase_suggestion_reactivated',
          'reactivated a suggestion for'
        );
      case 'DELETE_SUGGESTION':
        return t(
          'activity_phrase_suggestion_deleted',
          'deleted a suggestion for'
        );

      case 'SCREENSHOT_ADD':
        return hasKey(data)
          ? t('activity_phrase_screenshot_added', 'added a screenshot to')
          : t('activity_phrase_screenshot_added_plain', 'added a screenshot');
      case 'SCREENSHOT_DELETE':
        return hasKey(data)
          ? t('activity_phrase_screenshot_deleted', 'deleted a screenshot from')
          : t(
              'activity_phrase_screenshot_deleted_plain',
              'deleted a screenshot'
            );

      case 'TRANSLATION_LABEL_ASSIGN':
      case 'BATCH_ASSIGN_TRANSLATION_LABEL':
        return t('activity_phrase_labeled', 'labeled translation of');
      case 'BATCH_UNASSIGN_TRANSLATION_LABEL':
        return t('activity_phrase_unlabeled', 'removed a label from');
      case 'TRANSLATION_LABEL_CREATE':
        return t('activity_phrase_label_created', 'created label');
      case 'TRANSLATION_LABEL_UPDATE':
        return t('activity_phrase_label_updated', 'changed label');
      case 'TRANSLATION_LABEL_DELETE':
        return t('activity_phrase_label_deleted', 'deleted label');

      case 'TASK_CREATE':
      case 'TASKS_CREATE':
        return t('activity_phrase_task_created', 'created task');
      case 'TASK_UPDATE':
      case 'TASK_KEYS_UPDATE':
        return t('activity_phrase_task_updated', 'changed task');
      case 'TASK_FINISH':
        return t('activity_phrase_task_finished', 'finished task');
      case 'TASK_CLOSE':
        return t('activity_phrase_task_closed', 'closed task');
      case 'TASK_REOPEN':
        return t('activity_phrase_task_reopened', 'reopened task');

      case 'CREATE_LANGUAGE':
        return t('activity_phrase_language_added', 'added language');
      case 'EDIT_LANGUAGE':
        return t('activity_phrase_language_changed', 'changed language');
      case 'DELETE_LANGUAGE':
        return t('activity_phrase_language_deleted', 'deleted language');

      case 'CREATE_PROJECT':
        return t('activity_phrase_project_created', 'created a new project');
      case 'EDIT_PROJECT':
        return t(
          'activity_phrase_project_changed',
          'changed the project settings'
        );

      case 'WEBHOOK_CONFIG_CREATE':
        return t('activity_phrase_webhook_created', 'created a new webhook');
      case 'WEBHOOK_CONFIG_UPDATE':
        return t('activity_phrase_webhook_changed', 'changed the webhook');
      case 'WEBHOOK_CONFIG_DELETE':
        return t('activity_phrase_webhook_deleted', 'deleted the webhook');

      case 'CONTENT_DELIVERY_CONFIG_CREATE':
        return t(
          'activity_phrase_cd_created',
          'created a new content delivery'
        );
      case 'CONTENT_DELIVERY_CONFIG_UPDATE':
        return t(
          'activity_phrase_cd_changed',
          'changed the content delivery settings'
        );
      case 'CONTENT_DELIVERY_CONFIG_DELETE':
        return t('activity_phrase_cd_deleted', 'deleted the content delivery');
      case 'CONTENT_STORAGE_CREATE':
        return t(
          'activity_phrase_storage_created',
          'created a new content storage'
        );
      case 'CONTENT_STORAGE_UPDATE':
        return t(
          'activity_phrase_storage_changed',
          'changed the content storage settings'
        );
      case 'CONTENT_STORAGE_DELETE':
        return t(
          'activity_phrase_storage_deleted',
          'deleted the content storage'
        );

      case 'TRANSLATION_MEMORY_ASSIGN_PROJECT':
        return t(
          'activity_phrase_tm_assigned',
          'connected a translation memory'
        );
      case 'TRANSLATION_MEMORY_UNASSIGN_PROJECT':
        return t(
          'activity_phrase_tm_unassigned',
          'disconnected a translation memory'
        );
      case 'TRANSLATION_MEMORY_UPDATE_PROJECT_CONFIG':
        return t(
          'activity_phrase_tm_changed',
          'changed the translation memory settings'
        );

      case 'AI_PROMPT_CREATE':
        return t('activity_phrase_prompt_created', 'created a new AI prompt');
      case 'AI_PROMPT_UPDATE':
        return t('activity_phrase_prompt_changed', 'changed the AI prompt');
      case 'AI_PROMPT_DELETE':
        return t('activity_phrase_prompt_deleted', 'deleted the AI prompt');

      case 'BRANCH_CREATE':
        return t('activity_phrase_branch_created', 'created branch');
      case 'BRANCH_RENAME':
        return t('activity_phrase_branch_renamed', 'renamed branch');
      case 'BRANCH_DELETE':
        return t('activity_phrase_branch_deleted', 'deleted branch');
      case 'BRANCH_PROTECTION_CHANGE':
        return t(
          'activity_phrase_branch_protection',
          'changed protection of branch'
        );
      case 'BRANCH_MERGE':
        return t('activity_phrase_branch_merged', 'merged branch');

      case 'QA_ISSUE_IGNORE':
        return t('activity_phrase_qa_ignored', 'ignored a QA issue on');
      case 'QA_ISSUE_UNIGNORE':
        return t('activity_phrase_qa_unignored', 'restored a QA issue on');
    }
    return undefined;
  };
};
