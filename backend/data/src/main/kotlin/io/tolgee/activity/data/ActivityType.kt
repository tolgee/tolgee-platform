package io.tolgee.activity.data

import io.tolgee.activity.PublicParamsProvider
import io.tolgee.batch.BatchActivityParamsProvider
import io.tolgee.model.EntityWithId
import io.tolgee.model.Language
import kotlin.reflect.KClass

enum class ActivityType(
  val onlyCountsInList: Boolean = false,
  val restrictEntitiesInList: Array<KClass<out EntityWithId>>? = null,
  val paramsProvider: KClass<out PublicParamsProvider>? = null,
  val hideInList: Boolean = false,
  /**
   * If true, the activity will be saved even if it does
   * not contain any changes in fields market for activity logging
   */
  val saveWithoutModification: Boolean = false,
) {
  UNKNOWN,
  SET_TRANSLATION_STATE,
  SET_TRANSLATIONS,
  DISMISS_AUTO_TRANSLATED_STATE,
  SET_OUTDATED_FLAG,
  TRANSLATION_COMMENT_ADD,
  TRANSLATION_COMMENT_DELETE,
  TRANSLATION_COMMENT_EDIT,
  TRANSLATION_COMMENT_SET_STATE,
  SCREENSHOT_DELETE,
  SCREENSHOT_ADD,
  KEY_TAGS_EDIT,
  KEY_NAME_EDIT,
  KEY_DELETE(true),
  CREATE_KEY,
  COMPLEX_EDIT,
  IMPORT(true),
  CREATE_LANGUAGE,
  EDIT_LANGUAGE,
  DELETE_LANGUAGE(restrictEntitiesInList = arrayOf(Language::class)),

  /**
   * When language is deleted permanently, this happens asynchronously after language is deleted
   */
  HARD_DELETE_LANGUAGE(hideInList = true),
  CREATE_PROJECT,
  EDIT_PROJECT,
  NAMESPACE_EDIT,
  BATCH_PRE_TRANSLATE_BY_TM(onlyCountsInList = true, paramsProvider = BatchActivityParamsProvider::class),
  BATCH_MACHINE_TRANSLATE(true, paramsProvider = BatchActivityParamsProvider::class),
  AUTO_TRANSLATE(true, paramsProvider = BatchActivityParamsProvider::class),
  BATCH_CLEAR_TRANSLATIONS(true, paramsProvider = BatchActivityParamsProvider::class),
  BATCH_COPY_TRANSLATIONS(true, paramsProvider = BatchActivityParamsProvider::class),
  BATCH_SET_TRANSLATION_STATE(true, paramsProvider = BatchActivityParamsProvider::class),
  BATCH_TAG_KEYS(true, paramsProvider = BatchActivityParamsProvider::class),
  BATCH_UNTAG_KEYS(true, paramsProvider = BatchActivityParamsProvider::class),
  BATCH_SET_KEYS_NAMESPACE(true, paramsProvider = BatchActivityParamsProvider::class),
  AUTOMATION(onlyCountsInList = true, hideInList = true),
  CONTENT_DELIVERY_CONFIG_CREATE,
  CONTENT_DELIVERY_CONFIG_UPDATE,
  CONTENT_DELIVERY_CONFIG_DELETE,
  CONTENT_STORAGE_CREATE,
  CONTENT_STORAGE_UPDATE,
  CONTENT_STORAGE_DELETE,
  WEBHOOK_CONFIG_CREATE,
  WEBHOOK_CONFIG_UPDATE,
  WEBHOOK_CONFIG_DELETE,
  COMPLEX_TAG_OPERATION(onlyCountsInList = true),
  TASKS_CREATE,
  TASK_CREATE,
  TASK_UPDATE,
  TASK_KEYS_UPDATE,
  TASK_FINISH,
  TASK_CLOSE,
  TASK_REOPEN,
  TASK_KEY_UPDATE(hideInList = true),
}
