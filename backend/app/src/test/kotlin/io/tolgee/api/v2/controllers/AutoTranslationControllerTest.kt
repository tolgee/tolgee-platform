package io.tolgee.api.v2.controllers

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.AutoTranslateTestData
import io.tolgee.fixtures.MachineTranslationTest
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutoTranslationControllerTest : MachineTranslationTest() {
  companion object {
    private const val INITIAL_BUCKET_CREDITS = 150000L
  }

  lateinit var testData: AutoTranslateTestData

  @BeforeEach
  fun setup() {
    testData = AutoTranslateTestData()
    testData.disableAutoTranslating()
    this.projectSupplier = { testData.project }
    initMachineTranslationMocks()
    initMachineTranslationProperties(INITIAL_BUCKET_CREDITS)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `auto translates using MT`() {
    saveTestData()
    performProjectAuthPut(
      "keys/${testData.thisIsBeautifulKey.id}/auto-translate?" +
        "useMachineTranslation=true" +
        "&languages=de" +
        "&languages=es",
      null,
    ).andIsOk
    assertThat(testData.thisIsBeautifulKey.getLangTranslation(testData.spanishLanguage).text)
      .isEqualTo("Translated with Google")
    assertThat(testData.thisIsBeautifulKey.getLangTranslation(testData.germanLanguage).text)
      .isEqualTo("Translated with Google")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `auto translates manually only specified language`() {
    saveTestData()
    performProjectAuthPut(
      "keys/${testData.thisIsBeautifulKey.id}/auto-translate?" +
        "languages=${testData.spanishLanguage.tag}&" +
        "useMachineTranslation=true",
      null,
    ).andIsOk
    assertThat(testData.thisIsBeautifulKey.getLangTranslation(testData.spanishLanguage).text)
      .isEqualTo("Translated with Google")
    assertThat(testData.thisIsBeautifulKey.getLangTranslation(testData.germanLanguage).text)
      .isEqualTo("Es ist schön.")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `auto translates manually using TM`() {
    val another = testData.createAnotherThisIsBeautifulKey()
    saveTestData()
    performProjectAuthPut(
      "keys/${another.id}/auto-translate?" +
        "languages=${testData.germanLanguage.tag}&" +
        "useTranslationMemory=true",
      null,
    ).andIsOk
    val translation = another.getLangTranslation(testData.germanLanguage)
    assertThat(translation.mtProvider).isEqualTo(null)
    assertThat(translation.auto).isEqualTo(true)
  }

  @ProjectApiKeyAuthTestMethod
  @Test
  fun `works with API key`() {
    saveTestData()
    performProjectAuthPut(
      "keys/${testData.thisIsBeautifulKey.id}/auto-translate?" +
        "useMachineTranslation=true" +
        "&languages=de" +
        "&languages=es",
      null,
    ).andIsOk
    assertThat(testData.thisIsBeautifulKey.getLangTranslation(testData.spanishLanguage).text)
      .isEqualTo("Translated with Google")
    assertThat(testData.thisIsBeautifulKey.getLangTranslation(testData.germanLanguage).text)
      .isEqualTo("Translated with Google")
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  @Test
  fun `insufficient API key scope`() {
    saveTestData()
    performProjectAuthPut(
      "keys/${testData.thisIsBeautifulKey.id}/auto-translate?" +
        "useMachineTranslation=true" +
        "&languages=de" +
        "&languages=es",
      null,
    ).andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `cannot translate base`() {
    val another = testData.createAnotherThisIsBeautifulKey()
    saveTestData()
    performProjectAuthPut(
      "keys/${another.id}/auto-translate?" +
        "languages=en&" +
        "useTranslationMemory=true",
      null,
    ).andIsBadRequest.andHasErrorMessage(Message.CANNOT_TRANSLATE_BASE_LANGUAGE)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `doesn't work when no service provided`() {
    saveTestData()
    performProjectAuthPut("keys/${testData.thisIsBeautifulKey.id}/auto-translate", null).andIsBadRequest
  }

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }
}
