package io.tolgee.ee.api.v2.controllers.qa

import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.WebsocketTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WebsocketTest
class QaCheckPreviewWebSocketTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var qa: QaTestUtil

  @LocalServerPort
  private var port: Int = 0

  lateinit var testData: QaTestData
  lateinit var jwtToken: String

  private val wsHelpers = mutableListOf<QaPreviewWebSocketTestHelper>()

  private fun createWs(): QaPreviewWebSocketTestHelper {
    val ws = QaPreviewWebSocketTestHelper(port)
    wsHelpers.add(ws)
    return ws
  }

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaTestData()
    testDataService.saveTestData(testData.root)
    qa.testData = testData
    qa.saveDefaultQaConfig()
    userAccount = testData.user
    jwtToken = jwtService.emitToken(testData.user.id)
  }

  @AfterEach
  fun cleanup() {
    wsHelpers.forEach { it.close() }
    wsHelpers.clear()
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `returns empty translation issue for blank text`() {
    val ws = createWs()
    ws.connect()
    ws.sendInit(jwtToken, testData.project.id, null, "en")
    ws.sendText("")
    ws.waitForDone()

    val issues = ws.collectAllIssues()
    assertThat(issues).hasSize(1)
    assertThat(issues[0]["type"]).isEqualTo("EMPTY_TRANSLATION")
    assertThat(issues[0]["message"]).isEqualTo("qa_empty_translation")
    assertThat(issues[0]["positionStart"]).isEqualTo(0)
    assertThat(issues[0]["positionEnd"]).isEqualTo(0)
  }

  @Test
  fun `returns no issues for non-empty text`() {
    val ws = createWs()
    ws.connect()
    ws.sendInit(jwtToken, testData.project.id, null, "en")
    ws.sendText("Hello world")
    ws.waitForDone()

    val issues = ws.collectAllIssues()
    assertThat(issues).isEmpty()
  }

  @Test
  fun `closes connection with invalid token`() {
    val ws = createWs()
    ws.connect()
    ws.sendInit("invalid-token", testData.project.id, null, "en")
    ws.waitForClose()

    val error = ws.getError()
    assertThat(error).isNotNull
    assertThat(error!!["type"]).isEqualTo("error")
  }

  @Test
  fun `returns comparison issues when base translation exists`() {
    val ws = createWs()
    ws.connect()
    ws.sendInit(jwtToken, testData.project.id, testData.testKey.id, "fr")
    ws.sendText("bonjour monde")
    ws.waitForDone()

    val issues = ws.collectAllIssues()
    assertThat(issues).isNotEmpty

    val types = issues.map { it["type"] }
    assertThat(types).contains("CHARACTER_CASE_MISMATCH")
    assertThat(types).contains("PUNCTUATION_MISMATCH")

    val caseIssue = issues.first { it["type"] == "CHARACTER_CASE_MISMATCH" }
    assertThat(caseIssue["message"]).isEqualTo("qa_case_capitalize")

    val punctuationIssue = issues.first { it["type"] == "PUNCTUATION_MISMATCH" }
    assertThat(punctuationIssue["message"]).isEqualTo("qa_punctuation_add")
  }

  @Test
  fun `returns no comparison issues when keyId is absent`() {
    val ws = createWs()
    ws.connect()
    ws.sendInit(jwtToken, testData.project.id, null, "fr")
    ws.sendText("bonjour monde")
    ws.waitForDone()

    val issues = ws.collectAllIssues()
    assertThat(issues).isEmpty()
  }

  @Test
  fun `returns params in result for punctuation check`() {
    val ws = createWs()
    ws.connect()
    ws.sendInit(jwtToken, testData.project.id, testData.testKey.id, "fr")
    ws.sendText("Bonjour monde")
    ws.waitForDone()

    val issues = ws.collectAllIssues()
    val punctuationIssue = issues.first { it["type"] == "PUNCTUATION_MISMATCH" }

    @Suppress("UNCHECKED_CAST")
    val params = punctuationIssue["params"] as Map<String, String>
    assertThat(params["punctuation"]).isEqualTo(".")
  }

  @Test
  fun `returns no missing numbers when neither text has numbers`() {
    val ws = createWs()
    ws.connect()
    ws.sendInit(jwtToken, testData.project.id, testData.testKey.id, "fr")
    ws.sendText("Bonjour monde.")
    ws.waitForDone()

    // Both "Hello world." and "Bonjour monde." — matching case, matching punctuation, no numbers
    val issues = ws.collectAllIssues()
    assertThat(issues).isEmpty()
  }

  @Test
  fun `preview includes state from persisted issues`() {
    // Persist QA issues for the French translation
    qa.runChecksAndPersist(testData.frTranslation)

    // Ignore one issue
    val issueToIgnore = qa.getPersistedIssues(testData.frTranslation).first()
    qa.ignoreIssue(issueToIgnore)

    // Preview the same text — should include state status
    val ws = createWs()
    ws.connect()
    ws.sendInit(jwtToken, testData.project.id, testData.testKey.id, "fr")
    ws.sendText("bonjour monde")
    ws.waitForDone()

    val issues = ws.collectAllIssues()
    assertThat(issues).isNotEmpty

    val states = issues.map { it["state"] }
    assertThat(states).contains("IGNORED")
    assertThat(states).contains("OPEN")
  }

  @Test
  fun `preview without persisted issues has default state value`() {
    val ws = createWs()
    ws.connect()
    ws.sendInit(jwtToken, testData.project.id, null, "en")
    ws.sendText("")
    ws.waitForDone()

    val issues = ws.collectAllIssues()
    assertThat(issues).hasSize(1)
    assertThat(issues[0]["state"]).isEqualTo("OPEN")
  }

  @Test
  fun `sends results per check type`() {
    val ws = createWs()
    ws.connect()
    ws.sendInit(jwtToken, testData.project.id, testData.testKey.id, "fr")
    ws.sendText("bonjour monde")
    ws.waitForDone()

    val resultMessages = ws.collectResults()
    // Each result message should have a distinct checkType
    val checkTypes = resultMessages.map { it["checkType"] }
    assertThat(checkTypes).doesNotHaveDuplicates()

    // All messages should have a "result" type
    assertThat(resultMessages).allSatisfy {
      assertThat(it["type"]).isEqualTo("result")
      assertThat(it["checkType"]).isNotNull
      assertThat(it["issues"]).isNotNull
    }

    // "done" should be the last received message
    val allMessages = ws.receivedMessages.toList()
    assertThat(allMessages.last()["type"]).isEqualTo("done")
  }

  @Test
  fun `handles multiple text updates on same connection`() {
    val ws = createWs()
    ws.connect()
    ws.sendInit(jwtToken, testData.project.id, null, "en")

    // First text — has issues
    ws.sendText("")
    ws.waitForDone()
    val firstIssues = ws.collectAllIssues()
    assertThat(firstIssues).hasSize(1)
    assertThat(firstIssues[0]["type"]).isEqualTo("EMPTY_TRANSLATION")

    // Clear and send second text — no issues
    ws.receivedMessages.clear()
    ws.sendText("Hello world")
    ws.waitForDone()
    val secondIssues = ws.collectAllIssues()
    assertThat(secondIssues).isEmpty()
  }
}
