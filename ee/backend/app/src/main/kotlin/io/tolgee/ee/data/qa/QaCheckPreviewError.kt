package io.tolgee.ee.data.qa

data class QaCheckPreviewError(
  val type: QaPreviewMessageType = QaPreviewMessageType.ERROR,
  val message: String,
)
