package io.tolgee.component.machineTranslation.providers

import com.google.cloud.translate.Translate
import io.tolgee.component.machineTranslation.LanguageTagConvertor
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class GoogleTranslationProvider(
  private val googleMachineTranslationProperties: GoogleMachineTranslationProperties,
  private val translate: Translate?
) : MtValueProvider {
  override fun calculatePrice(text: String): Int {
    return text.length * 100
  }

  override val isEnabled: Boolean
    get() = !googleMachineTranslationProperties.apiKey.isNullOrEmpty()

  override fun translate(text: String, sourceLanguageTag: String, targetLanguageTag: String): String? {
    if (targetLanguageTag.suitableTag.isNullOrEmpty() || sourceLanguageTag.suitableTag.isNullOrEmpty()) {
      return null
    }

    return translateService.translate(
      text,
      Translate.TranslateOption.targetLanguage(targetLanguageTag.suitableTag),
      Translate.TranslateOption.sourceLanguage(sourceLanguageTag.suitableTag)
    ).translatedText
  }

  private val translateService by lazy {
    translate ?: throw IllegalStateException("Google Translate is not injected")
  }

  private val String.suitableTag: String?
    get() {
      return LanguageTagConvertor.findSuitableTag(languages, this)
    }

  companion object {
    val languages = arrayOf(
      "af",
      "sq",
      "am",
      "ar",
      "hy",
      "az",
      "eu",
      "be",
      "bn",
      "bs",
      "bg",
      "ca",
      "ceb",
      "ny",
      "zh-CN",
      "zh-TW",
      "co",
      "hr",
      "cs",
      "da",
      "nl",
      "en",
      "eo",
      "et",
      "tl",
      "fi",
      "fr",
      "fy",
      "gl",
      "ka",
      "de",
      "el",
      "gu",
      "ht",
      "ha",
      "haw",
      "iw",
      "hi",
      "hmn",
      "hu",
      "is",
      "ig",
      "id",
      "ga",
      "it",
      "ja",
      "jw",
      "kn",
      "kk",
      "km",
      "rw",
      "ko",
      "ku",
      "ky",
      "lo",
      "la",
      "lv",
      "lt",
      "lb",
      "mk",
      "mg",
      "ms",
      "ml",
      "mt",
      "mi",
      "mr",
      "mn",
      "my",
      "ne",
      "no",
      "or",
      "ps",
      "fa",
      "pl",
      "pt",
      "pa",
      "ro",
      "ru",
      "sm",
      "gd",
      "sr",
      "st",
      "sn",
      "sd",
      "si",
      "sk",
      "sl",
      "so",
      "es",
      "su",
      "sw",
      "sv",
      "tg",
      "ta",
      "tt",
      "te",
      "th",
      "tr",
      "tk",
      "uk",
      "ur",
      "ug",
      "uz",
      "vi",
      "cy",
      "xh",
      "yi",
      "yo",
      "zu",
      "he",
      "zh"
    )
  }
}
