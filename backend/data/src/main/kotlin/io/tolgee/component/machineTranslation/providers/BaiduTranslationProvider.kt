package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.configuration.tolgee.machineTranslation.BaiduMachineTranslationProperties
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class BaiduTranslationProvider(
  private val baiduMachineTranslationProperties: BaiduMachineTranslationProperties,
  private val baiduApiService: BaiduApiService,
) : AbstractMtValueProvider() {
  override val isEnabled: Boolean
    get() =
      !baiduMachineTranslationProperties.appId.isNullOrEmpty() &&
        !baiduMachineTranslationProperties.appSecret.isNullOrEmpty()

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    val result =
      baiduApiService.translate(
        params.text,
        getLanguageTag(params.sourceLanguageTag),
        getLanguageTag(params.targetLanguageTag),
      )
    return MtValueProvider.MtResult(
      result,
      params.text.length * 100,
    )
  }

  fun getLanguageTag(tag: String): String {
    return languageTagMap[tag.lowercase()] ?: tag.lowercase()
  }

  override val supportedLanguages =
    arrayOf(
      "yue",
      "yue-Hans",
      "yue-Hans-CN",
      "kor",
      "ko",
      "ko-KR",
      "th",
      "th-TH",
      "pt",
      "pt-PT",
      "pt-BR",
      "el",
      "el-GR",
      "bul",
      "bg",
      "bg-BG",
      "fin",
      "fi",
      "fi-FI",
      "slo",
      "sk",
      "sk-SK",
      "cht",
      "zh-Hant",
      "zh-Hant-HK",
      "zh-Hant-MO",
      "zh-Hant-TW",
      "zh-TW",
      "zh",
      "zh-Hans",
      "zh-Hans-CN",
      "zh-Hans-SG",
      "zh-Hans-HK",
      "zh-Hans-MO",
      "wyw",
      "fra",
      "fr",
      "fr-FR",
      "ara",
      "ar",
      "de",
      "de-DE",
      "nl",
      "nl-NL",
      "est",
      "et",
      "et-EE",
      "cs",
      "cs-CZ",
      "swe",
      "sl",
      "sl-SI",
      "sv",
      "sv-SE",
      "vie",
      "vi",
      "vi-VN",
      "en",
      "en-US",
      "en-GB",
      "jp",
      "ja",
      "ja-JP",
      "spa",
      "es",
      "es-ES",
      "ru",
      "ru-RU",
      "it",
      "it-IT",
      "pl",
      "pl-PL",
      "dan",
      "da",
      "da-DK",
      "rom",
      "ro",
      "ro-RO",
      "hu",
      "hu-HU",
    )

  override val formalitySupportingLanguages: Array<String>? = null

  // https://fanyi-api.baidu.com/doc/21
  // extracted commonly used only
  val languageTagMap =
    mapOf(
      // "yue"
      // "kor"
      "ko" to "kor",
      // "th"
      // "pt"
      // "el"
      // "bul"
      "bg" to "bul",
      // "fin"
      "fi" to "fin",
      // "slo"
      "sl" to "slo",
      // "sk"
      // "cht"
      "zh-hant" to "cht",
      "zh-tw" to "cht",
      // "zh"
      "zh-hans" to "zh",
      // "wyw"
      // "fra"
      "fr" to "fra",
      // "ara"
      "ar" to "ara",
      // "de"
      // "nl"
      // "est"
      "et" to "est",
      // "cs"
      // "swe"
      "sv" to "swe",
      // "vie"
      "vi" to "vie",
      // "en"
      // "jp"
      "ja" to "jp",
      // "spa"
      "es" to "spa",
      // "ru"
      // "it"
      // "pl"
      // "dan"
      "da" to "dan",
      // "rom"
      "ro" to "rom",
      // "hu"
    )
}
