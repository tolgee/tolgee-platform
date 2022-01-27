package io.tolgee.component.machineTranslation

interface MtValueProvider {
  val isEnabled: Boolean

  /**
   * Translates the text using the service
   */
  fun translate(text: String, sourceLanguageTag: String, targetLanguageTag: String): String?

  /**
   * Calculates credit price of the provider
   */
  fun calculatePrice(text: String): Int
}
