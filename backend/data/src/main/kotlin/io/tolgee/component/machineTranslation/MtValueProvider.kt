package io.tolgee.component.machineTranslation

import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams

interface MtValueProvider {
  val isEnabled: Boolean

  /**
   * Translates the text using the service
   */
  fun translate(params: ProviderTranslateParams): MtResult

  data class MtResult(
    var translated: String?,
    val price: Int,
    val contextDescription: String? = null
  )
}
