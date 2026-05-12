package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaGlossaryTerm

fun qaCheckParams(
  text: String,
  baseText: String? = null,
  baseLanguageTag: String? = null,
  languageTag: String = "en",
  isPlural: Boolean = false,
  textVariants: Map<String, String>? = null,
  textVariantOffsets: Map<String, Int>? = null,
  baseTextVariants: Map<String, String>? = null,
  maxCharLimit: Int? = null,
  icuPlaceholders: Boolean = true,
  glossaryTerms: List<QaGlossaryTerm>? = null,
) = QaCheckParams(
  baseText = baseText,
  text = text,
  baseLanguageTag = baseLanguageTag,
  languageTag = languageTag,
  isPlural = isPlural,
  textVariants = textVariants,
  textVariantOffsets = textVariantOffsets,
  baseTextVariants = baseTextVariants,
  maxCharLimit = maxCharLimit,
  icuPlaceholders = icuPlaceholders,
  glossaryTerms = glossaryTerms,
)
