package io.tolgee.ee.component.machineTranslation

import io.tolgee.component.EeSubscriptionInfoProvider
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.component.machineTranslation.providers.AbstractMtValueProvider
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.component.machineTranslation.providers.tolgee.CloudTolgeeTranslateApiService
import io.tolgee.component.machineTranslation.providers.tolgee.EeTolgeeTranslateApiService
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslateApiService
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslateParams
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslationProvider
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Primary
class TolgeeTranslationProviderImpl(
  @Lazy
  private val cloudTolgeeTranslateApiService: CloudTolgeeTranslateApiService,
  @Lazy
  private val eeTolgeeTranslateApiService: EeTolgeeTranslateApiService,
  private val eeSubscriptionInfoProvider: EeSubscriptionInfoProvider,
  private val publicBillingConfProvider: PublicBillingConfProvider,
) : AbstractMtValueProvider(), TolgeeTranslationProvider {
  override val isEnabled: Boolean get() = apiService != null

  override fun translateViaProvider(params: ProviderTranslateParams): MtValueProvider.MtResult {
    val apiServiceNotNull =
      apiService
        ?: throw IllegalStateException("Tolgee translation is not enabled")
    return apiServiceNotNull.translate(
      TolgeeTranslateParams(
        params.textRaw,
        params.keyName,
        params.sourceLanguageTag,
        params.targetLanguageTag,
        params.metadataOrThrow(),
        params.formality,
        params.isBatch,
        pluralForms = params.pluralForms,
        pluralFormExamples = params.pluralFormExamples,
      ),
    )
  }

  private val apiService: TolgeeTranslateApiService?
    get() {
      if (eeSubscriptionInfoProvider.isSubscribed()) {
        return eeTolgeeTranslateApiService
      }
      if (publicBillingConfProvider().enabled) {
        return cloudTolgeeTranslateApiService
      }
      return null
    }

  fun ProviderTranslateParams.metadataOrThrow(): Metadata {
    return this.metadata
      ?: throw IllegalArgumentException("Metadata must be set for Tolgee translation")
  }

  // empty array meaning all is supported
  override val supportedLanguages = arrayOf<String>()
  override val formalitySupportingLanguages = arrayOf<String>()

  override fun isLanguageSupported(tag: String): Boolean = true

  override fun isLanguageFormalitySupported(tag: String): Boolean = true
}
