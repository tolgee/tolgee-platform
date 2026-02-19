package io.tolgee.exceptions

import io.tolgee.constants.Message

class ExportCollidingKeysException(
  val pluralKey: String,
  val collidingKey: String,
  val suffix: String,
) : BadRequestException(
    Message.EXPORT_KEY_PLURAL_SUFFIX_COLLISION,
    listOf(pluralKey, collidingKey, suffix),
  )
