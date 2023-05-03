package io.tolgee.component.machineTranslation.providers

import io.tolgee.configuration.tolgee.machineTranslation.DeeplMachineTranslationProperties
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class DeeplTranslationProvider(
  private val deeplMachineTranslationProperties: DeeplMachineTranslationProperties,
  private val deeplApiService: DeeplApiService
) : AbstractMtValueProvider() {

  override val isEnabled: Boolean
    get() = !deeplMachineTranslationProperties.authKey.isNullOrEmpty()

  override fun translateViaProvider(text: String, sourceTag: String, targetTag: String): String? {
    return deeplApiService.translate(text, sourceTag.uppercase(), targetTag.uppercase())
  }

  override fun calculateProviderPrice(text: String): Int {
    return text.length * 100
  }

  override val supportedLanguages = arrayOf(
    "bg",
    "cs",
    "da",
    "de",
    "el",
    "en",
    "en-gb",
    "en-us",
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
    "pt-pt",
    "pt-br",
    "ro",
    "ru",
    "sk",
    "sl",
    "sv",
    "tr",
    "uk",
    "zh"
  )
}
