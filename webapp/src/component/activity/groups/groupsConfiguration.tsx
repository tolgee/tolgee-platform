import React from 'react';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type ActivityGroupTypeEnum =
  components['schemas']['ActivityGroupModel']['type'];

type GroupTypeConfiguration = {
  label: React.ReactNode;
};

export const groupsConfiguration: Record<
  ActivityGroupTypeEnum,
  GroupTypeConfiguration
> = {
  CREATE_KEY: {
    label: (
      <T
        keyName="activity_groups_type_create_key"
        defaultValue="Created keys"
      />
    ),
  },
  EDIT_KEY_NAME: {
    label: (
      <T
        keyName="activity_groups_type_edit_key_name"
        defaultValue="Renamed keys"
      />
    ),
  },
  DELETE_KEY: {
    label: (
      <T
        keyName="activity_groups_type_delete_key"
        defaultValue="Deleted keys"
      />
    ),
  },
  SET_TRANSLATION_STATE: {
    label: (
      <T
        keyName="activity_groups_type_set_translation_state"
        defaultValue="Changed translation states"
      />
    ),
  },
  REVIEW: {
    label: (
      <T
        keyName="activity_groups_type_review"
        defaultValue="Reviewed translations"
      />
    ),
  },
  SET_BASE_TRANSLATION: {
    label: (
      <T
        keyName="activity_groups_type_set_base_translation"
        defaultValue="Changed base translations"
      />
    ),
  },
  SET_TRANSLATIONS: {
    label: (
      <T
        keyName="activity_groups_type_set_translations"
        defaultValue="Set translations"
      />
    ),
  },
  DISMISS_AUTO_TRANSLATED_STATE: {
    label: (
      <T
        keyName="activity_groups_type_dismiss_auto_translated_state"
        defaultValue="Dismissed auto-translated states"
      />
    ),
  },
  SET_OUTDATED_FLAG: {
    label: (
      <T
        keyName="activity_groups_type_set_outdated_flag"
        defaultValue="Changed outdated flags"
      />
    ),
  },
  ADD_TRANSLATION_COMMENT: {
    label: (
      <T
        keyName="activity_groups_type_add_translation_comment"
        defaultValue="Added translation comments"
      />
    ),
  },
  DELETE_TRANSLATION_COMMENT: {
    label: (
      <T
        keyName="activity_groups_type_delete_translation_comment"
        defaultValue="Deleted translation comments"
      />
    ),
  },
  EDIT_TRANSLATION_COMMENT: {
    label: (
      <T
        keyName="activity_groups_type_edit_translation_comment"
        defaultValue="Edited translation comments"
      />
    ),
  },
  SET_TRANSLATION_COMMENT_STATE: {
    label: (
      <T
        keyName="activity_groups_type_set_translation_comment_state"
        defaultValue="Changed translation comment states"
      />
    ),
  },
  DELETE_SCREENSHOT: {
    label: (
      <T
        keyName="activity_groups_type_delete_screenshot"
        defaultValue="Deleted screenshots"
      />
    ),
  },
  ADD_SCREENSHOT: {
    label: (
      <T
        keyName="activity_groups_type_add_screenshot"
        defaultValue="Added screenshots"
      />
    ),
  },
  EDIT_KEY_TAGS: {
    label: (
      <T
        keyName="activity_groups_type_edit_key_tags"
        defaultValue="Edited key tags"
      />
    ),
  },
  IMPORT: {
    label: (
      <T keyName="activity_groups_type_import" defaultValue="Imported data" />
    ),
  },
  CREATE_LANGUAGE: {
    label: (
      <T
        keyName="activity_groups_type_create_language"
        defaultValue="Created languages"
      />
    ),
  },
  EDIT_LANGUAGE: {
    label: (
      <T
        keyName="activity_groups_type_edit_language"
        defaultValue="Edited languages"
      />
    ),
  },
  DELETE_LANGUAGE: {
    label: (
      <T
        keyName="activity_groups_type_delete_language"
        defaultValue="Deleted languages"
      />
    ),
  },
  CREATE_PROJECT: {
    label: (
      <T
        keyName="activity_groups_type_create_project"
        defaultValue="Created the project"
      />
    ),
  },
  EDIT_PROJECT: {
    label: (
      <T
        keyName="activity_groups_type_edit_project"
        defaultValue="Edited project settings"
      />
    ),
  },
  NAMESPACE_EDIT: {
    label: (
      <T
        keyName="activity_groups_type_namespace_edit"
        defaultValue="Edited namespaces"
      />
    ),
  },
  BATCH_PRE_TRANSLATE_BY_TM: {
    label: (
      <T
        keyName="activity_groups_type_batch_pre_translate_by_tm"
        defaultValue="Pre-translated by translation memory"
      />
    ),
  },
  BATCH_MACHINE_TRANSLATE: {
    label: (
      <T
        keyName="activity_groups_type_batch_machine_translate"
        defaultValue="Machine translated"
      />
    ),
  },
  AUTO_TRANSLATE: {
    label: (
      <T
        keyName="activity_groups_type_auto_translate"
        defaultValue="Auto-translated"
      />
    ),
  },
  BATCH_CLEAR_TRANSLATIONS: {
    label: (
      <T
        keyName="activity_groups_type_batch_clear_translations"
        defaultValue="Cleared translations"
      />
    ),
  },
  BATCH_COPY_TRANSLATIONS: {
    label: (
      <T
        keyName="activity_groups_type_batch_copy_translations"
        defaultValue="Copied translations"
      />
    ),
  },
  BATCH_SET_TRANSLATION_STATE: {
    label: (
      <T
        keyName="activity_groups_type_batch_set_translation_state"
        defaultValue="Changed translation states in batch"
      />
    ),
  },
  CONTENT_DELIVERY_CONFIG_CREATE: {
    label: (
      <T
        keyName="activity_groups_type_content_delivery_config_create"
        defaultValue="Created content delivery configurations"
      />
    ),
  },
  CONTENT_DELIVERY_CONFIG_UPDATE: {
    label: (
      <T
        keyName="activity_groups_type_content_delivery_config_update"
        defaultValue="Updated content delivery configurations"
      />
    ),
  },
  CONTENT_DELIVERY_CONFIG_DELETE: {
    label: (
      <T
        keyName="activity_groups_type_content_delivery_config_delete"
        defaultValue="Deleted content delivery configurations"
      />
    ),
  },
  CONTENT_STORAGE_CREATE: {
    label: (
      <T
        keyName="activity_groups_type_content_storage_create"
        defaultValue="Created content storages"
      />
    ),
  },
  CONTENT_STORAGE_UPDATE: {
    label: (
      <T
        keyName="activity_groups_type_content_storage_update"
        defaultValue="Updated content storages"
      />
    ),
  },
  CONTENT_STORAGE_DELETE: {
    label: (
      <T
        keyName="activity_groups_type_content_storage_delete"
        defaultValue="Deleted content storages"
      />
    ),
  },
  WEBHOOK_CONFIG_CREATE: {
    label: (
      <T
        keyName="activity_groups_type_webhook_config_create"
        defaultValue="Created webhook configurations"
      />
    ),
  },
  WEBHOOK_CONFIG_UPDATE: {
    label: (
      <T
        keyName="activity_groups_type_webhook_config_update"
        defaultValue="Updated webhook configurations"
      />
    ),
  },
  WEBHOOK_CONFIG_DELETE: {
    label: (
      <T
        keyName="activity_groups_type_webhook_config_delete"
        defaultValue="Deleted webhook configurations"
      />
    ),
  },
  EDIT_KEY_CHARACTER_LIMIT: {
    label: (
      <T
        keyName="activity_groups_type_edit_key_character_limit"
        defaultValue="Changed key character limits"
      />
    ),
  },
  SOFT_DELETE_KEY: {
    label: (
      <T
        keyName="activity_groups_type_soft_delete_key"
        defaultValue="Moved keys to trash"
      />
    ),
  },
  RESTORE_KEY: {
    label: (
      <T
        keyName="activity_groups_type_restore_key"
        defaultValue="Restored keys"
      />
    ),
  },
  HARD_DELETE_KEY: {
    label: (
      <T
        keyName="activity_groups_type_hard_delete_key"
        defaultValue="Permanently deleted keys"
      />
    ),
  },
  SET_TRANSLATION_LABELS: {
    label: (
      <T
        keyName="activity_groups_type_set_translation_labels"
        defaultValue="Changed translation labels"
      />
    ),
  },
  CREATE_TRANSLATION_LABEL: {
    label: (
      <T
        keyName="activity_groups_type_create_translation_label"
        defaultValue="Created labels"
      />
    ),
  },
  EDIT_TRANSLATION_LABEL: {
    label: (
      <T
        keyName="activity_groups_type_edit_translation_label"
        defaultValue="Edited labels"
      />
    ),
  },
  DELETE_TRANSLATION_LABEL: {
    label: (
      <T
        keyName="activity_groups_type_delete_translation_label"
        defaultValue="Deleted labels"
      />
    ),
  },
  CREATE_TASK: {
    label: (
      <T
        keyName="activity_groups_type_create_task"
        defaultValue="Created tasks"
      />
    ),
  },
  EDIT_TASK: {
    label: (
      <T keyName="activity_groups_type_edit_task" defaultValue="Edited tasks" />
    ),
  },
  FINISH_TASK: {
    label: (
      <T
        keyName="activity_groups_type_finish_task"
        defaultValue="Finished tasks"
      />
    ),
  },
  CLOSE_TASK: {
    label: (
      <T
        keyName="activity_groups_type_close_task"
        defaultValue="Closed tasks"
      />
    ),
  },
  REOPEN_TASK: {
    label: (
      <T
        keyName="activity_groups_type_reopen_task"
        defaultValue="Reopened tasks"
      />
    ),
  },
  UPDATE_TASK_KEYS: {
    label: (
      <T
        keyName="activity_groups_type_update_task_keys"
        defaultValue="Updated task keys"
      />
    ),
  },
  CREATE_GLOSSARY: {
    label: (
      <T
        keyName="activity_groups_type_create_glossary"
        defaultValue="Created glossaries"
      />
    ),
  },
  EDIT_GLOSSARY: {
    label: (
      <T
        keyName="activity_groups_type_edit_glossary"
        defaultValue="Edited glossaries"
      />
    ),
  },
  DELETE_GLOSSARY: {
    label: (
      <T
        keyName="activity_groups_type_delete_glossary"
        defaultValue="Deleted glossaries"
      />
    ),
  },
  IMPORT_GLOSSARY: {
    label: (
      <T
        keyName="activity_groups_type_import_glossary"
        defaultValue="Imported glossaries"
      />
    ),
  },
  CREATE_GLOSSARY_TERM: {
    label: (
      <T
        keyName="activity_groups_type_create_glossary_term"
        defaultValue="Created glossary terms"
      />
    ),
  },
  EDIT_GLOSSARY_TERM: {
    label: (
      <T
        keyName="activity_groups_type_edit_glossary_term"
        defaultValue="Edited glossary terms"
      />
    ),
  },
  DELETE_GLOSSARY_TERM: {
    label: (
      <T
        keyName="activity_groups_type_delete_glossary_term"
        defaultValue="Deleted glossary terms"
      />
    ),
  },
  SET_GLOSSARY_TERM_TRANSLATION: {
    label: (
      <T
        keyName="activity_groups_type_set_glossary_term_translation"
        defaultValue="Set glossary term translations"
      />
    ),
  },
  CREATE_TRANSLATION_MEMORY: {
    label: (
      <T
        keyName="activity_groups_type_create_translation_memory"
        defaultValue="Created translation memories"
      />
    ),
  },
  EDIT_TRANSLATION_MEMORY: {
    label: (
      <T
        keyName="activity_groups_type_edit_translation_memory"
        defaultValue="Edited translation memories"
      />
    ),
  },
  DELETE_TRANSLATION_MEMORY: {
    label: (
      <T
        keyName="activity_groups_type_delete_translation_memory"
        defaultValue="Deleted translation memories"
      />
    ),
  },
  CREATE_SUGGESTION: {
    label: (
      <T
        keyName="activity_groups_type_create_suggestion"
        defaultValue="Created suggestions"
      />
    ),
  },
  DELETE_SUGGESTION: {
    label: (
      <T
        keyName="activity_groups_type_delete_suggestion"
        defaultValue="Deleted suggestions"
      />
    ),
  },
  DECLINE_SUGGESTION: {
    label: (
      <T
        keyName="activity_groups_type_decline_suggestion"
        defaultValue="Declined suggestions"
      />
    ),
  },
  ACCEPT_SUGGESTION: {
    label: (
      <T
        keyName="activity_groups_type_accept_suggestion"
        defaultValue="Accepted suggestions"
      />
    ),
  },
  SET_SUGGESTION_ACTIVE: {
    label: (
      <T
        keyName="activity_groups_type_set_suggestion_active"
        defaultValue="Reactivated suggestions"
      />
    ),
  },
  CREATE_AI_PROMPT: {
    label: (
      <T
        keyName="activity_groups_type_create_ai_prompt"
        defaultValue="Created AI prompts"
      />
    ),
  },
  EDIT_AI_PROMPT: {
    label: (
      <T
        keyName="activity_groups_type_edit_ai_prompt"
        defaultValue="Edited AI prompts"
      />
    ),
  },
  DELETE_AI_PROMPT: {
    label: (
      <T
        keyName="activity_groups_type_delete_ai_prompt"
        defaultValue="Deleted AI prompts"
      />
    ),
  },
  CREATE_BRANCH: {
    label: (
      <T
        keyName="activity_groups_type_create_branch"
        defaultValue="Created branches"
      />
    ),
  },
  RENAME_BRANCH: {
    label: (
      <T
        keyName="activity_groups_type_rename_branch"
        defaultValue="Renamed branches"
      />
    ),
  },
  CHANGE_BRANCH_PROTECTION: {
    label: (
      <T
        keyName="activity_groups_type_change_branch_protection"
        defaultValue="Changed branch protection"
      />
    ),
  },
  MERGE_BRANCH: {
    label: (
      <T
        keyName="activity_groups_type_merge_branch"
        defaultValue="Merged branches"
      />
    ),
  },
  IGNORE_QA_ISSUE: {
    label: (
      <T
        keyName="activity_groups_type_ignore_qa_issue"
        defaultValue="Ignored QA issues"
      />
    ),
  },
  UNIGNORE_QA_ISSUE: {
    label: (
      <T
        keyName="activity_groups_type_unignore_qa_issue"
        defaultValue="Reopened QA issues"
      />
    ),
  },
};
