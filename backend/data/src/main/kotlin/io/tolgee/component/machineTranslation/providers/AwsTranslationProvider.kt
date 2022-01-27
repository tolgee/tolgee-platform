package io.tolgee.component.machineTranslation.providers

import com.amazonaws.services.translate.AmazonTranslate
import com.amazonaws.services.translate.model.TranslateTextRequest
import com.amazonaws.services.translate.model.TranslateTextResult
import io.tolgee.component.machineTranslation.LanguageTagConvertor
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.configuration.tolgee.machineTranslation.AwsMachineTranslationProperties
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class AwsTranslationProvider(
  private val awsMachineTranslationProperties: AwsMachineTranslationProperties,
  private val amazonTranslate: AmazonTranslate?
) : MtValueProvider {
  override fun calculatePrice(text: String): Int {
    return text.length * 100
  }

  override val isEnabled: Boolean
    get() = !awsMachineTranslationProperties.accessKey.isNullOrEmpty() &&
      !awsMachineTranslationProperties.secretKey.isNullOrEmpty()

  override fun translate(text: String, sourceLanguageTag: String, targetLanguageTag: String): String? {
    if (targetLanguageTag.suitableTag.isNullOrEmpty() || sourceLanguageTag.suitableTag.isNullOrEmpty()) {
      return null
    }

    val request: TranslateTextRequest = TranslateTextRequest()
      .withText(text)
      .withSourceLanguageCode(sourceLanguageTag.suitableTag)
      .withTargetLanguageCode(targetLanguageTag.suitableTag)

    val result: TranslateTextResult = translateService.translateText(request)
    return result.translatedText
  }

  private val translateService by lazy {
    amazonTranslate ?: throw IllegalStateException("AmazonTranslate is not injected")
  }

  private val String.suitableTag: String?
    get() {
      return LanguageTagConvertor.findSuitableTag(GoogleTranslationProvider.languages, this)
    }

  companion object {
    val supportedTags = arrayOf(
      "af",
      "sq",
      "am",
      "ar",
      "hy",
      "az",
      "bn",
      "bs",
      "bg",
      "ca",
      "zh",
      "zh-TW",
      "hr",
      "cs",
      "da ",
      "fa-AF",
      "nl ",
      "en",
      "et",
      "fa",
      "tl",
      "fi",
      "fr",
      "fr-CA",
      "ka",
      "de",
      "el",
      "gu",
      "ht",
      "ha",
      "he ",
      "hi",
      "hu",
      "is",
      "id ",
      "ga",
      "it",
      "ja",
      "kn",
      "kk",
      "ko",
      "lv",
      "lt",
      "mk",
      "ms",
      "ml",
      "mt",
      "mr",
      "mn",
      "no",
      "ps",
      "pl",
      "pt",
      "pt-PT",
      "pa",
      "ro",
      "ru",
      "sr",
      "si",
      "sk",
      "sl",
      "so",
      "es",
      "es-MX",
      "sw",
      "sv",
      "ta",
      "te",
      "th",
      "tr",
      "uk",
      "ur",
      "uz",
      "vi",
      "cy"
    )
  }
}
