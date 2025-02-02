package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.configuration.tolgee.machineTranslation.OpenaiMachineTranslationProperties
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class OpenaiTranslationProvider(
  private val openaiMachineTranslationProperties: OpenaiMachineTranslationProperties,
  private val openaiApiService: OpenaiApiService,
) : AbstractMtValueProvider() {
  override val isEnabled: Boolean
    get() = !openaiMachineTranslationProperties.apiKey.isNullOrEmpty()

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    val result =
      openaiApiService.translate(
        params.text,
        params.sourceLanguageTag,
        params.targetLanguageTag,
      )
    return MtValueProvider.MtResult(result, params.text.length * 100)
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
      "pt-pt",
      "pt-br",
      "ru",
    )

  override val supportedLanguages =
    arrayOf(
      "yue",
      "yue-hans",
      "yue-hans-cn",
      "kor",
      "ko",
      "ko-kr",
      "th",
      "th-th",
      "pt",
      "pt-pt",
      "pt-br",
      "el",
      "el-gr",
      "bul",
      "bg",
      "bg-bg",
      "fin",
      "fi",
      "fi-fi",
      "slo",
      "sk",
      "sk-sk",
      "cht",
      "zh-hant",
      "zh-hant-hk",
      "zh-hant-mo",
      "zh-hant-tw",
      "zh-tw",
      "zh",
      "zh-hans",
      "zh-hans-cn",
      "zh-hans-sg",
      "zh-hans-hk",
      "zh-hans-mo",
      "wyw",
      "fra",
      "fr",
      "fr-fr",
      "ara",
      "ar",
      "de",
      "de-de",
      "nl",
      "nl",
      "nl-nl",
      "est",
      "et",
      "et-ee",
      "cs",
      "cs-cz",
      "swe",
      "sl",
      "sl-si",
      "sv",
      "sv-se",
      "vie",
      "vi",
      "vi-vn",
      "en",
      "en-us",
      "en-gb",
      "jp",
      "ja",
      "ja-jp",
      "spa",
      "es",
      "es-es",
      "ru",
      "ru-ru",
      "it",
      "it-it",
      "pl",
      "pl-pl",
      "dan",
      "da",
      "da-dk",
      "rom",
      "ro",
      "ro-ro",
      "hu",
      "hu-hu",
    )
}
