package io.tolgee.autoTranslating

import com.amazonaws.services.translate.AmazonTranslate
import com.google.cloud.translate.Translate
import io.tolgee.MachineTranslationTest
import io.tolgee.constants.MtServiceType
import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.AutoTranslateTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import kotlin.system.measureTimeMillis

@SpringBootTest
@AutoConfigureMockMvc
class AutoTranslatingTest : ProjectAuthControllerTest("/v2/projects/"), MachineTranslationTest {

  companion object {
    private const val INITIAL_BUCKET_CREDITS = 150000L
    private const val TRANSLATED_WITH_GOOGLE_RESPONSE = "Translated with Google"
    private const val CREATE_KEY_NAME = "super_key"
    private const val THIS_IS_BEAUTIFUL_DE = "Es ist schön."
  }

  lateinit var testData: AutoTranslateTestData

  @Autowired
  @MockBean
  override lateinit var googleTranslate: Translate

  @Autowired
  @MockBean
  override lateinit var amazonTranslate: AmazonTranslate

  @BeforeEach
  fun setup() {
    testData = AutoTranslateTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
    initMachineTranslationMocks()
    initMachineTranslationProperties(INITIAL_BUCKET_CREDITS)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `auto translates new key`() {
    testUsingMtWorks()
    val expectedCost = "Hello".length * 100
    assertThat(mtCreditBucketService.getCreditBalance(testData.project))
      .isEqualTo(INITIAL_BUCKET_CREDITS - expectedCost)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `auto translates when base provided (non-existing)`() {
    performSetEnTranslation(testData.baseTranslationNotExistKey.name)

    val esTranslation = testData.baseTranslationNotExistKey.getLangTranslation(testData.spanishLanguage)
    val deTranslation = testData.baseTranslationNotExistKey.getLangTranslation(testData.germanLanguage)

    assertThat(esTranslation.text).isEqualTo("i am translated")
    assertThat(esTranslation.state).isEqualTo(TranslationState.TRANSLATED)
    assertThat(esTranslation.auto).isEqualTo(false)

    assertThat(deTranslation.text).isEqualTo(TRANSLATED_WITH_GOOGLE_RESPONSE)
    assertThat(deTranslation.state).isEqualTo(TranslationState.TRANSLATED)
    assertThat(deTranslation.auto).isEqualTo(true)
    assertThat(deTranslation.mtProvider).isEqualTo(MtServiceType.GOOGLE)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `auto translates when base provided (existing, but untranslated)`() {
    performSetEnTranslation(testData.baseTranslationUntranslated.name)

    val esTranslation = testData.baseTranslationUntranslated.getLangTranslation(testData.spanishLanguage).text
    val deTranslation = testData.baseTranslationUntranslated.getLangTranslation(testData.germanLanguage).text

    assertThat(esTranslation).isEqualTo("i am translated")
    assertThat(deTranslation).isEqualTo(TRANSLATED_WITH_GOOGLE_RESPONSE)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `auto translates using TM`() {
    testUsingTmWorks()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `it translates in parallel`() {
    testData.generateManyLanguages()
    initMachineTranslationMocks(500)
    testDataService.saveTestData(testData.root)

    val time = measureTimeMillis {
      performCreateKey(
        mapOf(
          "en" to "This is it",
        )
      )
    }

    assertThat(time).isLessThan(2000)

    val translations = keyService.get(testData.project.id, CREATE_KEY_NAME).translations.toList()
    assertThat(translations).hasSize(9)
    assertThat(translations[4].text).isEqualTo("Translated with Amazon")
    assertThat(translations[5].text).isEqualTo("Translated with Google")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test tm enabled`() {
    performSetConfig(true, false)
    testUsingTmWorks()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test mt disabled`() {
    performSetConfig(true, false)
    testUsingMtDoesNotWork()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test mt enabled`() {
    performSetConfig(false, true)
    testUsingMtWorks()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `config test tm disabled`() {
    performSetConfig(false, true)
    testUsingTmDoesNotWork()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `doesn't fail when out of credits`() {
    initMachineTranslationProperties(0)
    performCreateHalloKeyWithEnAndDeTranslations()
    assertThat(
      keyService
        .get(testData.project.id, CREATE_KEY_NAME)
        .translations
        .find { it.language == testData.spanishLanguage }
    )
      .isNull()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `doesn't consume credits when not enough for all`() {
    testData.generateLanguagesWithDifferentPrimaryServices()
    testDataService.saveTestData(testData.root)
    initMachineTranslationProperties(700)
    performCreateHalloKeyWithEnAndDeTranslations()
    assertThat(
      keyService
        .get(testData.project.id, CREATE_KEY_NAME)
        .translations
        .find { it.language == testData.spanishLanguage }
    )
      .isNull()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `it returns autoTranslateConfig`() {
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
    val esTranslation = keyService.get(testData.project.id, CREATE_KEY_NAME)
      .translations
      .find { it.language == testData.spanishLanguage }

    assertThat(esTranslation).isNull()
  }

  private fun getCreatedEsTranslation() = keyService.get(testData.project.id, CREATE_KEY_NAME)
    .getLangTranslation(testData.spanishLanguage).text

  private fun performCreateHalloKeyWithEnAndDeTranslations() {
    performCreateKey(
      mapOf(
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

  private fun getCreatedDeTranslation() = keyService.get(testData.project.id, CREATE_KEY_NAME)
    .getLangTranslation(testData.germanLanguage).text

  private fun createAnotherThisIsBeautifulKey() {
    performCreateKey(
      mapOf(
        "en" to "This is beautiful",
      )
    )
  }

  private fun performSetConfig(usingTm: Boolean, usingMt: Boolean) {
    performProjectAuthPut(
      "auto-translation-settings",
      mapOf(
        "usingTranslationMemory" to usingTm,
        "usingMachineTranslation" to usingMt
      )
    ).andIsOk
  }

  private fun performCreateKey(translations: Map<String, String>) {
    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = CREATE_KEY_NAME,
        translations = translations
      )
    ).andIsCreated
  }

  private fun Key.getLangTranslation(lang: Language): Translation {
    return keyService.get(this.id).translations.find {
      it.language == lang
    } ?: throw IllegalStateException("Translation not found")
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
}
