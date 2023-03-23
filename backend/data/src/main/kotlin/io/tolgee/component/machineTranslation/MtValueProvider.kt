package io.tolgee.component.machineTranslation

import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams

interface MtValueProvider {
  val isEnabled: Boolean

  /**
   * Translates the text using the service
   */
  fun translate(params: ProviderTranslateParams): String?

  /**
   * Calculates credit price of the provider
   */
  fun calculatePrice(params: ProviderTranslateParams): Int
}
