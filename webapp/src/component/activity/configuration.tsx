import { T } from '@tolgee/react';
import { ActivityOptions, ActivityTypeEnum } from './types';

export const actionsConfiguration: Partial<
  Record<ActivityTypeEnum, ActivityOptions>
> = {
  CREATE_PROJECT: {
    label(params) {
      return <T keyName="activity_create_project" params={params} />;
    },
  },
  SET_TRANSLATIONS: {
    label(params) {
      return <T keyName="activity_set_translation" params={params} />;
    },
    entities: { Translation: true },
  },
  DISMISS_AUTO_TRANSLATED_STATE: {
    label(params) {
      return (
        <T keyName="activity_dismiss_auto_translated_state" params={params} />
      );
    },
    entities: { Translation: true },
  },
  SET_TRANSLATION_STATE: {
    label(params) {
      return <T keyName="activity_set_translation_state" params={params} />;
    },
    entities: { Translation: true },
  },
  SET_OUTDATED_FLAG: {
    label(params) {
      return <T keyName="activity_set_outdated_flag" params={params} />;
    },
    entities: { Translation: true },
  },
  KEY_DELETE: {
    label(params) {
      return <T keyName="activity_key_delete" params={params} />;
    },
    entities: { Key: [] },
    titleReferences: [],
  },
  KEY_NAME_EDIT: {
    label(params) {
      return <T keyName="activity_key_name_edit" params={params} />;
    },
    entities: { Key: ['name'] },
  },
  CREATE_KEY: {
    label(params) {
      return <T keyName="activity_create_key" params={params} />;
    },
    entities: {
      Translation: ['text', 'autoTranslation'],
      KeyMeta: true,
      Key: [],
    },
  },
  COMPLEX_EDIT: {
    label(params) {
      return <T keyName="activity_complex_edit" params={params} />;
    },
    entities: {
      Translation: true,
      Key: ['name', 'namespace'],
      Screenshot: true,
    },
  },
  DELETE_LANGUAGE: {
    label(params) {
      return <T keyName="activity_delete_language" params={params} />;
    },
    entities: { Language: [] },
  },
  EDIT_LANGUAGE: {
    label(params) {
      return <T keyName="activity_edit_language" params={params} />;
    },
    entities: { Language: true },
  },
  CREATE_LANGUAGE: {
    label(params) {
      return <T keyName="activity_create_language" params={params} />;
    },
    entities: { Language: [] },
  },
  SCREENSHOT_ADD: {
    label(params) {
      return <T keyName="activity_screenshot_add" params={params} />;
    },
    entities: { Screenshot: [] },
  },
  SCREENSHOT_DELETE: {
    label(params) {
      return <T keyName="activity_screenshot_delete" params={params} />;
    },
    entities: { Screenshot: [] },
  },
  TRANSLATION_COMMENT_ADD: {
    label(params) {
      return <T keyName="activity_translation_comment_add" params={params} />;
    },
    entities: { TranslationComment: ['text'] },
  },
  TRANSLATION_COMMENT_SET_STATE: {
    label(params) {
      return (
        <T keyName="activity_translation_comment_set_state" params={params} />
      );
    },
    entities: { TranslationComment: true },
    titleReferences: ['key'],
  },
  TRANSLATION_COMMENT_DELETE: {
    label(params) {
      return (
        <T keyName="activity_translation_comment_delete" params={params} />
      );
    },
    entities: { TranslationComment: ['text'] },
  },
  IMPORT: {
    label(params) {
      return <T keyName="activity_import" params={params} />;
    },
  },
  KEY_TAGS_EDIT: {
    label(params) {
      return <T keyName="activity_key_tags_edit" params={params} />;
    },
    entities: { KeyMeta: true },
  },
  TRANSLATION_HISTORY_ADD: {
    label(params) {
      return <T keyName="activity_translation_history_add" params={params} />;
    },
  },
  TRANSLATION_HISTORY_MODIFY: {
    label(params) {
      return (
        <T keyName="activity_translation_history_modify" params={params} />
      );
    },
  },
  EDIT_PROJECT: {
    label(params) {
      return <T keyName="activity_edit_project" params={params} />;
    },
    entities: { Project: ['name'] },
  },
  NAMESPACE_EDIT: {
    label(params) {
      return <T keyName="activity_edit_namespace" params={params} />;
    },
    entities: { Namespace: true },
  },
  BATCH_PRE_TRANSLATE_BY_TM: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_pre_translate_by_tm"
          params={params}
        />
      );
    },
    entities: { Params: true },
  },
  BATCH_MACHINE_TRANSLATE: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_machine_translate"
          params={params}
        />
      );
    },
    entities: { Params: true },
  },
  BATCH_SET_TRANSLATION_STATE: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_set_translation_state"
          params={params}
        />
      );
    },
    entities: { Params: true },
    compactFieldCount: 2,
  },
  BATCH_COPY_TRANSLATIONS: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_copy_translations"
          params={params}
        />
      );
    },
    entities: { Params: ['targetLanguageIds'] },
  },
  BATCH_CLEAR_TRANSLATIONS: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_clear_translations"
          params={params}
        />
      );
    },
    entities: { Params: true },
  },
  BATCH_TAG_KEYS: {
    label(params) {
      return <T keyName="activity_batch_operation_tag_keys" params={params} />;
    },
    entities: { Params: true },
  },
  BATCH_UNTAG_KEYS: {
    label(params) {
      return (
        <T keyName="activity_batch_operation_untag_keys" params={params} />
      );
    },
    entities: { Params: true },
  },
  BATCH_SET_KEYS_NAMESPACE: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_set_keys_namespace"
          params={params}
        />
      );
    },
    entities: { Params: true },
  },
  AUTO_TRANSLATE: {
    label(params) {
      return (
        <T keyName="activity_batch_operation_auto_translate" params={params} />
      );
    },
  },
  CONTENT_DELIVERY_CONFIG_CREATE: {
    label(params) {
      return (
        <T keyName="activity_content_delivery_config_create" params={params} />
      );
    },
    entities: {
      ContentDeliveryConfig: [],
    },
  },
  CONTENT_DELIVERY_CONFIG_UPDATE: {
    label(params) {
      return (
        <T keyName="activity_content_delivery_config_update" params={params} />
      );
    },
    entities: {
      ContentDeliveryConfig: [],
    },
  },
  CONTENT_DELIVERY_CONFIG_DELETE: {
    label(params) {
      return (
        <T keyName="activity_content_delivery_config_delete" params={params} />
      );
    },
    entities: {
      ContentDeliveryConfig: [],
    },
  },
  CONTENT_STORAGE_CREATE: {
    label(params) {
      return <T keyName="activity_content_storage_create" params={params} />;
    },
    entities: {
      ContentStorage: [],
    },
  },
  CONTENT_STORAGE_UPDATE: {
    label(params) {
      return <T keyName="activity_content_storage_update" params={params} />;
    },
    entities: {
      ContentStorage: [],
    },
  },
  CONTENT_STORAGE_DELETE: {
    label(params) {
      return <T keyName="activity_content_storage_delete" params={params} />;
    },
    entities: {
      ContentStorage: [],
    },
  },
  WEBHOOK_CONFIG_CREATE: {
    label(params) {
      return <T keyName="activity_webhook_config_create" params={params} />;
    },
    entities: {
      WebhookConfig: [],
    },
  },
  WEBHOOK_CONFIG_UPDATE: {
    label(params) {
      return <T keyName="activity_webhook_config_update" params={params} />;
    },
    entities: {
      WebhookConfig: [],
    },
  },
  WEBHOOK_CONFIG_DELETE: {
    label(params) {
      return <T keyName="activity_webhook_config_delete" params={params} />;
    },
    entities: {
      WebhookConfig: [],
    },
  },
  COMPLEX_TAG_OPERATION: {
    label(params) {
      return <T keyName="activity_complex_tag_operation" params={params} />;
    },
  },
  TASK_CREATE: {
    label() {
      return <T keyName="activity_task_create" />;
    },
    entities: {
      Task: true,
    },
  },
  TASKS_CREATE: {
    label() {
      return <T keyName="activity_tasks_create" />;
    },
    entities: {
      Task: true,
    },
  },
  TASK_UPDATE: {
    label() {
      return <T keyName="activity_task_update" />;
    },
    entities: {
      Task: true,
    },
  },
  TASK_FINISH: {
    label() {
      return <T keyName="activity_task_finish" />;
    },
    entities: {
      Task: [],
    },
  },
  TASK_CLOSE: {
    label() {
      return <T keyName="activity_task_close" />;
    },
    entities: {
      Task: [],
    },
  },
  TASK_REOPEN: {
    label() {
      return <T keyName="activity_task_reopen" />;
    },
    entities: {
      Task: [],
    },
  },
  TASK_KEYS_UPDATE: {
    label() {
      return <T keyName="activity_task_keys_update" />;
    },
    entities: {
      Task: [],
    },
  },
};
