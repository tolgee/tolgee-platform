package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.configuration.tolgee.machineTranslation.DeeplMachineTranslationProperties
import io.tolgee.model.mtServiceConfig.Formality
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class DeeplTranslationProvider(
  private val deeplMachineTranslationProperties: DeeplMachineTranslationProperties,
  private val deeplApiService: DeeplApiService,
) : AbstractMtValueProvider() {
  override val isEnabled: Boolean
    get() = !deeplMachineTranslationProperties.authKey.isNullOrEmpty()

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    val result =
      deeplApiService.translate(
        params.text,
        params.sourceLanguageTag.uppercase(),
        params.targetLanguageTag.uppercase(),
        getFormality(params),
      )

    return MtValueProvider.MtResult(
      result,
      params.text.length * 100,
    )
  }

  private fun getFormality(params: ProviderTranslateParams): Formality {
    if (!isFormalitySupported(params.targetLanguageTag)) {
      return Formality.DEFAULT
    }
    return params.formality ?: Formality.DEFAULT
  }

  override val formalitySupportingLanguages =
    arrayOf(
      "de",
      "es",
      "fr",
      "it",
      "ja",
      "nl",
      "pl",
      "pt-PT",
      "pt-BR",
      "ru",
    )

  override val supportedLanguages =
    arrayOf(
      "ar",
      "bg",
      "cs",
      "da",
      "de",
      "el",
      "en",
      "en-GB",
      "en-US",
      "es",
      "et",
      "fi",
      "fr",
      "hu",
      "it",
      "id",
      "ja",
      "ko",
      "lt",
      "lv",
      "nb",
      "nl",
      "pl",
      "pt",
      "pt-PT",
      "pt-BR",
      "ro",
      "ru",
      "sk",
      "sl",
      "sv",
      "tr",
      "uk",
      "zh",
      "zh-Hans",
      "zh-Hant",
    )
}
