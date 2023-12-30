package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.configuration.tolgee.machineTranslation.TolgeeMachineTranslationProperties
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

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    return tolgeeTranslateApiService.translate(
      TolgeeTranslateApiService.Companion.TolgeeTranslateParams(
        params.textRaw,
        params.keyName,
        params.sourceLanguageTag,
        params.targetLanguageTag,
        params.metadataOrThrow(),
        params.formality,
        params.isBatch,
      ),
    )
  }

  fun ProviderTranslateParams.metadataOrThrow(): Metadata {
    if (metadata == null) {
      throw IllegalArgumentException("Metadata must be set for Tolgee translation")
    }
    return metadata
  }

  override val supportedLanguages = null
  override val formalitySupportingLanguages = null

  override fun isLanguageSupported(tag: String): Boolean = true

  override fun isLanguageFormalitySupported(tag: String): Boolean = true
}
