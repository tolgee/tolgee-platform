package io.tolgee.ee.api.v2.controllers.qa

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.QaLanguageAwareChecksTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.WebsocketTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WebsocketTest
class QaLanguageAwarePreviewWebSocketTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @LocalServerPort
  private var port: Int = 0

  private lateinit var testData: QaLanguageAwareChecksTestData
  private lateinit var ws: QaPreviewWebSocketTestHelper

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaLanguageAwareChecksTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    ws = QaPreviewWebSocketTestHelper(port)
  }

  @AfterEach
  fun cleanup() {
    ws.close()
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `preview treats hash and the plural argument as the same number`() {
    previewIssues("pending-edits", "nl", "{count, plural, one {# bewerking} other {# bewerkingen}}").assert.isEmpty()
  }

  @Test
  fun `preview accepts the plural argument where the base uses hash`() {
    previewIssues(
      "selected-items",
      "nl",
      "{count, plural, one {{count} item geselecteerd} other {{count} items geselecteerd}}",
    ).assert.isEmpty()
  }

  @Test
  fun `preview still reports a count dropped from a plural form`() {
    previewIssues("pending-edits", "nl", "{count, plural, one {# bewerking} other {bewerkingen}}")
      .assert
      .containsExactly("INCONSISTENT_PLACEHOLDERS:qa_placeholders_missing:other")
  }

  @Test
  fun `preview accepts spelled-out numbers in Arabic single-number forms`() {
    previewIssues(
      "documents-count",
      "ar",
      "{count, plural, zero {لا توجد مستندات} one {مستند واحد} two {مستندان} " +
        "few {# مستندات} many {# مستندًا} other {# مستند}}",
    ).assert.isEmpty()
  }

  @Test
  fun `preview suggests punctuation of the translation language`() {
    val issue = previewRawIssues("are-you-sure", "ar", "هل أنت متأكد").single()

    issue["message"].assert.isEqualTo("qa_punctuation_add")
    issue["replacement"].assert.isEqualTo("؟")
  }

  private fun previewIssues(
    keyName: String,
    languageTag: String,
    text: String,
  ): List<String> {
    return previewRawIssues(keyName, languageTag, text).map {
      listOfNotNull(it["type"], it["message"], it["pluralVariant"]).joinToString(":")
    }
  }

  private fun previewRawIssues(
    keyName: String,
    languageTag: String,
    text: String,
  ): List<Map<String, Any?>> {
    val key = testData.translations.getValue(keyName to languageTag).key
    ws.connect()
    ws.sendInit(jwtService.emitToken(testData.user.id), testData.project.id, key.id, languageTag)
    ws.sendText(text)
    ws.waitForDone()
    return ws.collectAllIssues()
  }
}
