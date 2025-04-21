package io.tolgee.component.machineTranslation

import io.tolgee.constants.MtServiceType
import io.tolgee.model.mtServiceConfig.Formality

class FakedMtResultProvider(
  private val sourceLanguageTag: String,
  private val targetLanguageTag: String,
  private val text: String,
  private val formality: Formality = Formality.DEFAULT,
  private val serviceType: MtServiceType,
) {
  constructor(params: TranslationParams) : this(
    params.sourceLanguageTag,
    params.targetLanguageTag,
    params.text,
    params.serviceInfo.formality ?: Formality.DEFAULT,
    params.serviceInfo.serviceType,
  )

  fun get(): MtValueProvider.MtResult {
    val fakedText =
      "$text translated ${formalityIndicator}with ${serviceType.name} " +
        "from $sourceLanguageTag to $targetLanguageTag"

    return MtValueProvider.MtResult(
      translated = fakedText,
      price = text.length * 100,
      contextDescription = null,
    )
  }

  private val formalityIndicator: String
    get() {
      if ((formality) !== Formality.DEFAULT) {
        return "$formality "
      }
      return ""
    }
}
