package io.tolgee.ee.service.qa

data class QaCheckParams(
  val baseText: String?,
  val text: String,
  val baseLanguageTag: String?,
  val languageTag: String,
  val isPlural: Boolean = false,
  val textVariants: Map<String, String>? = null,
  val textVariantOffsets: Map<String, Int>? = null,
  val baseTextVariants: Map<String, String>? = null,
  val activeVariant: String? = null,
  val maxCharLimit: Int? = null,
  val icuPlaceholders: Boolean = true,
)
