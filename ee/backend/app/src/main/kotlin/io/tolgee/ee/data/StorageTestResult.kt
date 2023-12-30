package io.tolgee.ee.data

import io.tolgee.constants.Message
import java.io.Serializable

data class StorageTestResult(
  val success: Boolean,
  val message: Message? = null,
  val params: List<Serializable?>? = null,
)
