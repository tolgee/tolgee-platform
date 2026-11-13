package io.tolgee.machineTranslation

import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.data.AutoTranslateTestData
import io.tolgee.fixtures.MachineTranslationTest
import io.tolgee.service.machineTranslation.MachineTranslationParams
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.annotation.DirtiesContext

/**
 * The MT cache reports a cache hit as `actualPrice = 0`, so a second walk over the plural forms
 * bills nothing. Whether the second walk hits depends on Spring's TransactionAwareCacheManagerProxy:
 * inside a transaction the first walk's `put` is deferred to after commit, with no transaction it
 * lands immediately. Both are real entry points (batch/auto-translate vs the streaming suggestion
 * endpoint, which translates on Dispatchers.IO), so the price must not depend on it.
 */
@SpringBootTest(properties = ["tolgee.cache.enabled=true"])
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ContextRecreatingTest
class PluralMtCreditChargeTest : MachineTranslationTest() {
  companion object {
    /**
     * Google is billed per character, so this is the length of the two prepared German forms
     * (`<x id="tolgee-number">1</x> dog` and `<x id="tolgee-number">10</x> dogs`) times 100.
     */
    private const val TWO_FORMS_PRICE = 6400
  }

  lateinit var testData: AutoTranslateTestData

  @BeforeEach
  fun setup() {
    testData = AutoTranslateTestData()
    testDataService.saveTestData(testData.root)
    initMachineTranslationMocks()
    initMachineTranslationProperties(1000000)
  }

  @Test
  fun `charges the full price with and without an active transaction`() {
    val withoutTransaction = translateOutsideTransaction("{count, plural, one {# dog} other {# dogs}}")
    val withTransaction = translateInsideTransaction("{count, plural, one {# cat} other {# cats}}")

    withoutTransaction.assert.isEqualTo(TWO_FORMS_PRICE)
    withTransaction.assert.isEqualTo(TWO_FORMS_PRICE)
  }

  @Test
  fun `calls the provider once per plural form`() {
    Mockito.clearInvocations(googleTranslate)
    translateOutsideTransaction("{count, plural, one {# dog} other {# dogs}}")

    verify(googleTranslate, times(2)).translate(any<String>(), any(), any(), any())
  }

  @Test
  fun `calls the provider once per plural form inside a transaction`() {
    Mockito.clearInvocations(googleTranslate)
    translateInsideTransaction("{count, plural, one {# cat} other {# cats}}")

    verify(googleTranslate, times(2)).translate(any<String>(), any(), any(), any())
  }

  private fun translateOutsideTransaction(baseText: String): Int {
    return mtService
      .getMtTranslator(testData.project.id, false)
      .translate(listOf(params(baseText)))
      .single()
      .actualPrice
  }

  private fun translateInsideTransaction(baseText: String): Int {
    return mtService
      .getMachineTranslations(testData.project.id, false, params(baseText))
      .single()
      .actualPrice
  }

  private fun params(baseText: String) =
    MachineTranslationParams(
      baseTranslationText = baseText,
      targetLanguageId = testData.germanLanguage.id,
      desiredServices = setOf(MtServiceType.GOOGLE),
    )
}
