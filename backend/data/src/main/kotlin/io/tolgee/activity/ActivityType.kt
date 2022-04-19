package io.tolgee.activity

import io.tolgee.model.EntityWithId
import io.tolgee.model.key.Key
import kotlin.reflect.KClass

enum class ActivityType(
  val onlyCountsInList: Boolean = false,
  val restrictEntitiesInList: Array<KClass<out EntityWithId>>? = null
) {
  UNKNOWN,
  SET_TRANSLATION_STATE,
  SET_TRANSLATIONS,
  DISMISS_AUTO_TRANSLATED_STATE,
  TRANSLATION_COMMENT_ADD,
  TRANSLATION_COMMENT_DELETE,
  TRANSLATION_COMMENT_EDIT,
  TRANSLATION_COMMENT_SET_STATE,
  SCREENSHOT_DELETE,
  SCREENSHOT_ADD,
  KEY_TAGS_EDIT,
  KEY_NAME_EDIT,
  KEY_DELETE(restrictEntitiesInList = arrayOf(Key::class)),
  CREATE_KEY,
  COMPLEX_EDIT,
  IMPORT(true),
  CREATE_LANGUAGE,
  EDIT_LANGUAGE,
  DELETE_LANGUAGE,
  CREATE_PROJECT,
  EDIT_PROJECT,
}
