package io.tolgee.model.views

import io.tolgee.constants.Message
import java.util.*

interface JobErrorMessagesView {
  val batchJobId: Long
  val errorMessage: Message
  val updatedAt: Date
}
