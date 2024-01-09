package io.tolgee.component.machineTranslation.providers.tolgee

import io.tolgee.component.machineTranslation.MtValueProvider

interface CloudTolgeeTranslateApiService : TolgeeTranslateApiService {
  override fun translate(params: TolgeeTranslateParams): MtValueProvider.MtResult

  companion object {
    const val BUCKET_KEY = "tolgee-translate-rate-limit"
  }
}
