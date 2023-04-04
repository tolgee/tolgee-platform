package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.configuration.tolgee.machineTranslation.TolgeeMachineTranslationProperties
import io.tolgee.helpers.IcuParamsReplacer
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TolgeeTranslationProvider(
  private val tolgeeMachineTranslationProperties: TolgeeMachineTranslationProperties,
  private val tolgeeTranslateApiService: TolgeeTranslateApiService,
) : AbstractMtValueProvider() {

  override val isEnabled: Boolean
    get() = !tolgeeMachineTranslationProperties.url.isNullOrBlank()

  override fun translateViaProvider(params: ProviderTranslateParams): String? {
    return tolgeeTranslateApiService.translate(
      TolgeeTranslateApiService.Companion.TolgeeTranslateParams(
        params.text,
        params.sourceLanguageTag,
        params.targetLanguageTag,
        params.metadataOrThrow()
      )
    )
  }

  fun ProviderTranslateParams.metadataOrThrow(): Metadata {
    if (metadata == null) {
      throw IllegalArgumentException("Metadata must be set for Tolgee translation")
    }
    return metadata
  }

  override fun calculateProviderPrice(text: String): Int {
    return text.length * 100
  }

  override val supportedLanguages = null

  override fun prepareText(string: String): IcuParamsReplacer.ReplaceIcuResult {
    return IcuParamsReplacer.doNothing(string)
  }
}
