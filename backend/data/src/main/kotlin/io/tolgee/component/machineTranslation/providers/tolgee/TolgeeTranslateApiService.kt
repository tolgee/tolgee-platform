package io.tolgee.component.machineTranslation.providers.tolgee

import io.tolgee.component.machineTranslation.MtValueProvider

interface TolgeeTranslateApiService {
  fun translate(params: TolgeeTranslateParams): MtValueProvider.MtResult
}
