package io.tolgee.autoTranslating

import io.tolgee.MachineTranslationTest
import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.data.AutoTranslateTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TranslationState
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.system.measureTimeMillis

@SpringBootTest
@AutoConfigureMockMvc
class AutoTranslatingTest : MachineTranslationTest() {
  companion object {
    private const val INITIAL_BUCKET_CREDITS = 150000L
    private const val TRANSLATED_WITH_GOOGLE_RESPONSE = "Translated with Google"
    private const val THIS_IS_BEAUTIFUL_DE = "Es ist schön."
  }

  lateinit var testData: AutoTranslateTestData

  @BeforeEach
  fun setup() {
    testData = AutoTranslateTestData()
    this.projectSupplier = { testData.project }
    initMachineTranslationMocks()
    initMachineTranslationProperties(INITIAL_BUCKET_CREDITS)
  }

  @ProjectJWTAuthTestMethod
  @Test
  @Transactional
  fun `auto translates new key`() {
    saveTestData()
    testUsingMtWorks()
    val expectedCost = "Hello".length * 100
    assertThat(mtCreditBucketService.getCreditBalances(testData.project).creditBalance)
      .isEqualTo(INITIAL_BUCKET_CREDITS - expectedCost)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `auto translates when base provided (non-existing)`() {
    saveTestData()
    performSetEnTranslation(testData.baseTranslationNotExistKey.name)

    executeInNewTransaction {
      val esTranslation = testData.baseTranslationNotExistKey.getLangTranslation(testData.spanishLanguage)
      val deTranslation = testData.baseTranslationNotExistKey.getLangTranslation(testData.germanLanguage)

      assertThat(esTranslation.text).isEqualTo("i am translated")
      assertThat(esTranslation.state).isEqualTo(TranslationState.TRANSLATED)
      assertThat(esTranslation.auto).isEqualTo(false)

      assertThat(deTranslation.text).isEqualTo(TRANSLATED_WITH_GOOGLE_RESPONSE)
      assertThat(deTranslation.outdated).isFalse
      assertThat(deTranslation.state).isEqualTo(TranslationState.TRANSLATED)
      assertThat(deTranslation.auto).isEqualTo(true)
      assertThat(deTranslation.mtProvider).isEqualTo(MtServiceType.GOOGLE)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  @Transactional
  fun `auto translates when base provided (existing, but untranslated)`() {
    saveTestData()
    performSetEnTranslation(testData.baseTranslationUntranslated.name)

    val esTranslation = testData.baseTranslationUntranslated.getLangTranslation(testData.spanishLanguage).text
    val deTranslation = testData.baseTranslationUntranslated.getLangTranslation(testData.germanLanguage).text

    assertThat(esTranslation).isEqualTo("i am translated")
    assertThat(deTranslation).isEqualTo(TRANSLATED_WITH_GOOGLE_RESPONSE)
  }

  @ProjectJWTAuthTestMethod
  @Test
  @Transactional
  fun `auto translates using TM`() {
    saveTestData()
    testUsingTmWorks()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `it translates in parallel`() {
    testData.generateManyLanguages()
    initMachineTranslationMocks(500)
    saveTestData()
    val time = measureTimeMillis {
      performCreateKey(
        translations = mapOf(
          "en" to "This is it",
        )
      )
    }
    assertThat(time).isLessThan(2000)

    executeInNewTransaction {
      val translations = keyService.get(testData.project.id, CREATE_KEY_NAME, null).translations
        .toList().sortedBy { it.text }
      assertThat(translations).hasSize(9)
      assertThat(translations[1].text).isEqualTo("Translated with Amazon")
      assertThat(translations[5].text).isEqualTo("Translated with Google")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test tm enabled`() {
    saveTestData()
    performSetConfig(true, false)
    testUsingTmWorks()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test mt disabled`() {
    saveTestData()
    performSetConfig(true, false)
    testUsingMtDoesNotWork()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test mt enabled`() {
    saveTestData()
    performSetConfig(false, true)
    testUsingMtWorks()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test tm disabled`() {
    saveTestData()
    performSetConfig(false, true)
    testUsingTmDoesNotWork()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `doesn't fail when out of credits`() {
    saveTestData()
    initMachineTranslationProperties(0)
    performCreateHalloKeyWithEnAndDeTranslations()
    transactionTemplate.execute {
      assertThat(
        keyService
          .get(testData.project.id, CREATE_KEY_NAME, null)
          .translations
          .find { it.language == testData.spanishLanguage }
      )
        .isNull()
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `consumes last positive credits`() {
    testData.generateLanguagesWithDifferentPrimaryServices()
    saveTestData()
    initMachineTranslationProperties(700)
    performCreateHalloKeyWithEnAndDeTranslations()
    transactionTemplate.execute {
      assertThat(
        keyService
          .get(testData.project.id, CREATE_KEY_NAME, null)
          .translations
          .find {
            it.language == testData.spanishLanguage
          }
      ).isNotNull
    }

    verify(googleTranslate, times(2)).translate(any<String>(), any())

    performCreateKey("yay", mapOf("en" to "yay"))

    verify(googleTranslate, times(2)).translate(any<String>(), any())

    val balance = mtCreditBucketService.getCreditBalances(testData.project)
    balance.creditBalance.assert.isEqualTo(0)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `it returns autoTranslateConfig`() {
    saveTestData()
    performProjectAuthGet("auto-translation-settings").andIsOk.andAssertThatJson {
      node("usingTranslationMemory").isEqualTo(true)
      node("usingMachineTranslation").isEqualTo(true)
    }
  }

  private fun testUsingMtWorks() {
    performCreateHalloKeyWithEnAndDeTranslations()
    val esTranslation = getCreatedEsTranslation()
    assertThat(esTranslation).isEqualTo(TRANSLATED_WITH_GOOGLE_RESPONSE)
  }

  private fun testUsingMtDoesNotWork() {
    performCreateHalloKeyWithEnAndDeTranslations()

    transactionTemplate.execute {
      val esTranslation = keyService.get(testData.project.id, CREATE_KEY_NAME, null)
        .translations
        .find { it.language == testData.spanishLanguage }

      assertThat(esTranslation).isNull()
    }
  }

  private fun getCreatedEsTranslation() = keyService.get(testData.project.id, CREATE_KEY_NAME, null)
    .getLangTranslation(testData.spanishLanguage).text

  private fun performCreateHalloKeyWithEnAndDeTranslations() {
    performCreateKey(
      translations = mapOf(
        "en" to "Hello",
        "de" to "Hallo"
      )
    )
  }

  private fun testUsingTmWorks() {
    createAnotherThisIsBeautifulKey()
    val deTranslation = getCreatedDeTranslation()
    assertThat(deTranslation).isEqualTo(THIS_IS_BEAUTIFUL_DE)
  }

  private fun testUsingTmDoesNotWork() {
    createAnotherThisIsBeautifulKey()
    val deTranslation = getCreatedDeTranslation()
    assertThat(deTranslation).isNotEqualTo(THIS_IS_BEAUTIFUL_DE)
  }

  private fun getCreatedDeTranslation() = keyService.get(testData.project.id, CREATE_KEY_NAME, null)
    .getLangTranslation(testData.germanLanguage).text

  private fun performSetConfig(usingTm: Boolean, usingMt: Boolean) {
    performProjectAuthPut(
      "auto-translation-settings",
      mapOf(
        "usingTranslationMemory" to usingTm,
        "usingMachineTranslation" to usingMt
      )
    ).andIsOk
  }

  private fun performSetEnTranslation(key: String) {
    performProjectAuthPut(
      "translations",
      SetTranslationsWithKeyDto(
        key = key,
        translations = mapOf("en" to "Hello")
      )
    )
  }

  fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }
}
