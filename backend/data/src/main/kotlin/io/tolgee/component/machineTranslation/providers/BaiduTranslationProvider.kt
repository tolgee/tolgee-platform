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
    "sv",
    "sv-se",
    "vie",
    "vi" ,
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

  // https://fanyi-api.baidu.com/doc/21
  // extracted commonly used only
  val languageTagMap = mapOf(
 // "yue"
    "yue-hans" to "yue",
    "yue-hans-cn" to "yue",
 // "kor"
    "ko" to "kor",
    "ko-kr" to "kor",
 // "th"
    "th-th" to "th",
 // "pt"
    "pt-pt" to "pt",
    "pt-br" to "pt",
 // "el"
    "el-gr" to "el",
 // "bul"
    "bg" to "bul",
    "bg-bg" to "bul",
 // "fin"
    "fi" to "fin",
    "fi-fi" to "fin",
 // "slo"
    "sk" to "slo",
    "sk-sk" to "slo",
 // "cht"
    "zh-hant" to "cht",
    "zh-hant-hk" to "cht",
    "zh-hant-mo" to "cht",
    "zh-hant-tw" to "cht",
    "zh-tw" to "cht",
 // "zh"
    "zh-hans" to "zh",
    "zh-hans-cn" to "zh",
    "zh-hans-sg" to "zh",
    "zh-hans-hk" to "zh",
    "zh-hans-mo" to "zh",
 // "wyw"
 // "fra"
    "fr" to "fra",
    "fr-fr" to "fra",
 // "ara"
    "ar" to "ara",
 // "de"
    "de-de" to "de",
 // "nl"
    "nl" to "nl",
    "nl-nl" to "nl",
 // "est"
    "et" to "est",
    "et-ee" to "est",
 // "cs"
    "cs-cz" to "cs",
 // "swe"
    "sv" to "swe",
    "sv-se" to "swe",
 // "vie"
    "vi" to "vie",
    "vi-vn" to "vie",
 // "en"
    "en-us" to "en",
    "en-gb" to "en",
 // "jp"
    "ja" to "jp",
    "ja-jp" to "jp",
 // "spa"
    "es" to "spa",
    "es-es" to "spa",
 // "ru"
    "ru-ru" to "ru",
 // "it"
    "it-it" to "it",
 // "pl"
    "pl-pl" to "pl",
 // "dan"
    "da" to "dan",
    "da-dk" to "dan",
 // "rom"
    "ro" to "rom",
    "ro-ro" to "rom",
 // "hu"
    "hu-hu" to "hu",
  )
}
