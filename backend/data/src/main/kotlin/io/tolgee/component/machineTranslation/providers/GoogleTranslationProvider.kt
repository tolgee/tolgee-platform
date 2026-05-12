package io.tolgee.component.machineTranslation.providers

import com.google.cloud.translate.Translate
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class GoogleTranslationProvider(
  private val googleMachineTranslationProperties: GoogleMachineTranslationProperties,
  private val translate: Translate?,
) : AbstractMtValueProvider() {
  override val isEnabled: Boolean
    get() = !googleMachineTranslationProperties.apiKey.isNullOrEmpty()

  /**
   * The Cloud Translation NMT model (used by [Translate]) only recognises `zh-CN`/`zh`
   * for Simplified and `zh-TW` for Traditional Chinese — the script-based tags
   * `zh-Hans`/`zh-Hant` are documented for the Translation LLM model, not NMT, and
   * silently fall back to Simplified when sent to NMT.
   *
   * docs: https://cloud.google.com/translate/docs/languages
   */
  override fun getSuitableTag(tag: String): String? {
    if (tag.equals("zh-Hant", ignoreCase = true)) {
      return super.getSuitableTag("zh-TW")
    }
    if (tag.equals("zh-Hans", ignoreCase = true)) {
      return super.getSuitableTag("zh-CN")
    }
    return super.getSuitableTag(tag)
  }

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    val result =
      translateService
        .translate(
          params.text,
          Translate.TranslateOption.sourceLanguage(params.sourceLanguageTag),
          Translate.TranslateOption.targetLanguage(params.targetLanguageTag),
          Translate.TranslateOption.format("text"),
        ).translatedText
    return MtValueProvider.MtResult(
      result,
      params.text.length * 100,
    )
  }

  private val translateService by lazy {
    translate ?: throw IllegalStateException("Google Translate is not injected")
  }

  override val supportedLanguages =
    arrayOf(
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
      "zh",
      "as",
      "ay",
      "bm",
      "bho",
      "dv",
      "doi",
      "ee",
      "gn",
      "ha",
      "ilo",
      "rw",
      "gom",
      "kri",
      "ln",
      "lg",
      "lb",
      "mai",
      "mni-Mtei",
      "lus",
      "om",
      "qu",
      "gd",
      "nso",
      "st",
      "tg",
      "ti",
      "ts",
      "ak",
    )
  override val formalitySupportingLanguages: Array<String>? = null
}
