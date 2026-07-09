package io.tolgee.unit

import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties

class TranslationFiltersVerbatimParamsTest {
  @Test
  fun `every pattern param binds verbatim`() {
    val patternParams =
      TranslationFilters::class
        .memberProperties
        .map { it.name }
        .filter { it.matches(Regex("filter(No)?\\w*Pattern")) }
    patternParams.assert.isNotEmpty
    TranslationFilters.VERBATIM_LIST_PARAMS.assert.containsAll(patternParams)
  }
}
