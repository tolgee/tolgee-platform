package io.tolgee.hateoas.machineTranslation

import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType

class StreamedSuggestionItem(
  val serviceType: MtServiceType,
  val result: TranslationItemModel?,
  val promptId: Long? = null,
  val errorMessage: Message? = null,
  val errorParams: List<Any?>? = null,
  val errorException: String? = null,
)
