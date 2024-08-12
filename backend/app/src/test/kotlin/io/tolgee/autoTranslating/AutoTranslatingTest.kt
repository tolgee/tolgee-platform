package io.tolgee.autoTranslating

import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.data.AutoTranslateTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.*
import io.tolgee.model.enums.TranslationState
import io.tolgee.testing.ContextRecreatingTest
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
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ContextRecreatingTest
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
  fun `auto translates new key`() {
    saveTestData()
    testUsingMtWorks()
    val expectedCost = "Hello".length * 100
    waitForNotThrowing {
      assertThat(mtCreditBucketService.getCreditBalances(testData.project.organizationOwner.id).creditBalance)
        .isEqualTo(INITIAL_BUCKET_CREDITS - expectedCost)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `auto translates when base provided (non-existing)`() {
    saveTestData()
    performSetEnTranslation(testData.baseTranslationNotExistKey.name)

    waitForNotThrowing {
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
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `auto translates when base provided (existing, but untranslated)`() {
    saveTestData()
    performSetEnTranslation(testData.baseTranslationUntranslated.name)

    waitForNotThrowing {
      val esTranslation = testData.baseTranslationUntranslated.getLangTranslation(testData.spanishLanguage).text
      val deTranslation = testData.baseTranslationUntranslated.getLangTranslation(testData.germanLanguage).text

      assertThat(esTranslation).isEqualTo("i am translated")
      assertThat(deTranslation).isEqualTo(TRANSLATED_WITH_GOOGLE_RESPONSE)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `auto translates using TM`() {
    saveTestData()
    testUsingTmWorks()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test tm enabled`() {
    saveTestData()
    performSetConfig(true, false, false)
    testUsingTmWorks()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test mt disabled`() {
    saveTestData()
    performSetConfig(true, false, false, testData.spanishLanguage.id)
    testUsingMtDoesNotWork()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test mt enabled`() {
    saveTestData()
    performSetConfig(false, true, false)
    testUsingMtWorks()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test tm disabled`() {
    saveTestData()
    performSetConfig(false, true, false)
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
          .find { it.language == testData.spanishLanguage },
      )
        .isNull()
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `consumes last positive credits`() {
    saveTestData()
    initMachineTranslationProperties(700)
    performCreateHalloKeyWithEnAndDeTranslations()

    waitForSpanishTranslationSet(CREATE_KEY_NAME)

    performCreateKey(
      keyName = "jaj",
      translations =
        mapOf(
          "en" to "Hello2",
          "de" to "Hallo2",
        ),
    )
    waitForSpanishTranslationSet("jaj")

    verify(googleTranslate, times(2)).translate(any<String>(), any(), any(), any())

    val balance = mtCreditBucketService.getCreditBalances(testData.project.organizationOwner.id)
    balance.creditBalance.assert.isEqualTo(0)
  }

  private fun waitForSpanishTranslationSet(keyName: String) {
    waitForNotThrowing(pollTime = 200) {
      transactionTemplate.execute {
        val translations =
          keyService
            .get(testData.project.id, keyName, null)
            .translations
        val spanishTranslation =
          translations
            .find {
              it.language == testData.spanishLanguage
            }
        spanishTranslation?.text.isNullOrBlank().assert.isFalse()
      }
    }
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

  @ProjectJWTAuthTestMethod
  @Test
  fun `it returns per language settings`() {
    saveTestData()
    performProjectAuthGet("per-language-auto-translation-settings").andIsOk.andAssertThatJson {
      node("_embedded.configs").isArray.hasSize(2)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `it sets and returns per language settings`() {
    saveTestData()
    performProjectAuthPut(
      "per-language-auto-translation-settings",
      listOf(
        mapOf(
          "languageId" to null,
          "usingTranslationMemory" to false,
          "usingMachineTranslation" to false,
        ),
        mapOf(
          "languageId" to testData.germanLanguage.id,
          "usingTranslationMemory" to false,
          "usingMachineTranslation" to false,
        ),
        mapOf(
          "languageId" to testData.spanishLanguage.id,
          "usingTranslationMemory" to false,
          "usingMachineTranslation" to false,
        ),
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded.configs").isArray.hasSize(3)
    }

    performProjectAuthGet("per-language-auto-translation-settings").andIsOk.andAssertThatJson {
      node("_embedded.configs") {
        isArray.hasSize(3)
        node("[0].usingTranslationMemory").isEqualTo(false)
        node("[0].usingMachineTranslation").isEqualTo(false)
      }
    }
  }

  private fun testUsingMtWorks() {
    performCreateHalloKeyWithEnAndDeTranslations()
    waitForNotThrowing {
      val esTranslation = getCreatedEsTranslation()
      assertThat(esTranslation).isEqualTo(TRANSLATED_WITH_GOOGLE_RESPONSE)
    }
  }

  private fun testUsingMtDoesNotWork() {
    performCreateHalloKeyWithEnAndDeTranslations()
    Thread.sleep(2000)
    transactionTemplate.execute {
      val esTranslation =
        keyService.get(testData.project.id, CREATE_KEY_NAME, null)
          .translations
          .find { it.language == testData.spanishLanguage }

      assertThat(esTranslation).isNull()
    }
  }

  private fun getCreatedEsTranslation() =
    keyService.get(testData.project.id, CREATE_KEY_NAME, null)
      .getLangTranslation(testData.spanishLanguage).text

  private fun performCreateHalloKeyWithEnAndDeTranslations(keyName: String = CREATE_KEY_NAME) {
    performCreateKey(
      keyName = keyName,
      translations =
        mapOf(
          "en" to "Hello",
          "de" to "Hallo",
        ),
    )
  }

  private fun testUsingTmWorks() {
    createAnotherThisIsBeautifulKey()
    waitForNotThrowing {
      val deTranslation = getCreatedDeTranslation()
      assertThat(deTranslation).isEqualTo(THIS_IS_BEAUTIFUL_DE)
    }
  }

  private fun testUsingTmDoesNotWork() {
    createAnotherThisIsBeautifulKey()
    Thread.sleep(2000)
    waitForNotThrowing {
      val deTranslation = getCreatedDeTranslation()
      assertThat(deTranslation).isNotEqualTo(THIS_IS_BEAUTIFUL_DE)
    }
  }

  private fun getCreatedDeTranslation() =
    keyService.get(testData.project.id, CREATE_KEY_NAME, null)
      .getLangTranslation(testData.germanLanguage).text

  private fun performSetConfig(
    usingTm: Boolean,
    usingMt: Boolean,
    enableForImport: Boolean,
    languageId: Long? = null,
  ) {
    performProjectAuthPut(
      "per-language-auto-translation-settings",
      listOf(
        mapOf(
          "languageId" to languageId,
          "usingTranslationMemory" to usingTm,
          "usingMachineTranslation" to usingMt,
          "enableForImport" to enableForImport,
        ),
      ),
    ).andIsOk
  }

  private fun performSetEnTranslation(key: String) {
    performProjectAuthPut(
      "translations",
      SetTranslationsWithKeyDto(
        key = key,
        translations = mapOf("en" to "Hello"),
      ),
    )
  }

  fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }
}
