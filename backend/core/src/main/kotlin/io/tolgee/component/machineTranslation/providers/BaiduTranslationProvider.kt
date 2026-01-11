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
