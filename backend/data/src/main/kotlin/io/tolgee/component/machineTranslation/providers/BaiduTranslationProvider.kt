package io.tolgee.component.machineTranslation.providers

import io.tolgee.configuration.tolgee.machineTranslation.BaiduMachineTranslationProperties
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class BaiduTranslationProvider(
  private val baiduMachineTranslationProperties: BaiduMachineTranslationProperties,
  private val baiduApiService: BaiduApiService
) : AbstractMtValueProvider() {

  override val isEnabled: Boolean
    get() = !baiduMachineTranslationProperties.appId.isNullOrEmpty() &&
      !baiduMachineTranslationProperties.appSecret.isNullOrEmpty()

  override fun translateViaProvider(text: String, sourceTag: String, targetTag: String): String? {
    return baiduApiService.translate(text, getLanguageTag(sourceTag), getLanguageTag(targetTag))
  }

  override fun calculateProviderPrice(text: String): Int {
    return text.length * 100
  }

  fun getLanguageTag(tag: String): String {
    return languageTagMap[tag.lowercase()] ?: tag.lowercase()
  }

  override val supportedLanguages = arrayOf(
    "yue",
    "kor",
    "ko",
    "th",
    "pt",
    "el",
    "bul",
    "bg",
    "fin",
    "fi",
    "slo",
    "sk",
    "cht",
    "zh-hant",
    "zh-tw",
    "zh",
    "zh-hans",
    "wyw",
    "fra",
    "fr",
    "ara",
    "ar",
    "de",
    "nl",
    "est",
    "et",
    "cs",
    "swe",
    "sv",
    "vie",
    "vi",
    "en",
    "jp",
    "ja",
    "spa",
    "es",
    "ru",
    "it",
    "pl",
    "dan",
    "da",
    "rom",
    "ro",
    "hu",
  )

  // https://fanyi-api.baidu.com/doc/21
  // extracted commonly used only
  val languageTagMap = mapOf(
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
    "sk" to "slo",
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
    "nl" to "nl",
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
