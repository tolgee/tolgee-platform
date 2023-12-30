package io.tolgee.exceptions

import io.tolgee.constants.Message
import java.io.Serializable

@Suppress("UNCHECKED_CAST")
class LanguageNotPermittedException(
  val languageIds: List<Long>,
  val languageTags: List<String>? = null,
) : PermissionException(Message.LANGUAGE_NOT_PERMITTED, listOf(languageIds, languageTags) as List<Serializable?>?)
