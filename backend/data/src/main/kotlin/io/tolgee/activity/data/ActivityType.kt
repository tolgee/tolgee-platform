package io.tolgee.activity.data

import io.tolgee.model.EntityWithId
import io.tolgee.model.Language
import kotlin.reflect.KClass

enum class ActivityType(
  val onlyCountsInList: Boolean = false,
  val restrictEntitiesInList: Array<KClass<out EntityWithId>>? = null,
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
  CREATE_PROJECT,
  EDIT_PROJECT,
  NAMESPACE_EDIT,
  BATCH_AUTO_TRANSLATE(true),
  BATCH_CLEAR_TRANSLATIONS(true),
  BATCH_COPY_TRANSLATIONS(true),
  BATCH_SET_TRANSLATION_STATE(true),
  BATCH_TAG_KEYS(true),
  BATCH_UNTAG_KEYS(true),
  BATCH_SET_KEYS_NAMESPACE(true)
  ;
}
