package io.tolgee.ee.aiReview

import io.tolgee.development.testDataBuilder.data.AiReviewFilterTestData
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsOk
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

class AiReviewTranslationsFilterTest : AbstractAiReviewTest() {
  @Autowired
  lateinit var jsonMapper: ObjectMapper

  @Autowired
  lateinit var aiReviewRuleRepository: AiReviewRuleRepository

  override fun createTestData() = AiReviewFilterTestData()

  private val data get() = testData as AiReviewFilterTestData

  @BeforeEach
  fun save() {
    saveTestData()
  }

  private fun keyNames(query: String): List<String> {
    val url = "/v2/projects/$projectId/translations?languages=en,de,cs,fr&sort=keyName&size=100&$query"
    val json = jsonMapper.readTree(performAuthGet(url).andIsOk.andGetContentAsString)
    val names =
      json
        .path("_embedded")
        .path("keys")
        .items()
        .map { it.path("keyName").asString() }
    json
      .path("page")
      .path("totalElements")
      .asLong()
      .assert
      .isEqualTo(names.size.toLong())
    return names
  }

  @Test
  fun `filters by each outcome`() {
    keyNames("filterAiReviewOutcome=AUTO_REVIEWED").assert.containsExactly("ui-key")
    keyNames("filterAiReviewOutcome=BELOW_THRESHOLD").assert.containsExactly("ui-key")
    keyNames("filterAiReviewOutcome=WOULD_AUTO_REVIEW").assert.containsExactly("legal-key")
    keyNames("filterAiReviewOutcome=WOULD_NOT_AUTO_REVIEW").assert.containsExactly("checkout-key")
    keyNames("filterAiReviewOutcome=FAILED").assert.containsExactly("plain-key")
  }

  @Test
  fun `outcomes are combined with OR`() {
    keyNames("filterAiReviewOutcome=FAILED&filterAiReviewOutcome=WOULD_AUTO_REVIEW")
      .assert
      .containsExactly("legal-key", "plain-key")
  }

  @Test
  fun `applies to the AI review filter languages only`() {
    keyNames("filterAiReviewOutcome=BELOW_THRESHOLD&filterAiReviewInLang=de").assert.isEmpty()
    keyNames("filterAiReviewOutcome=BELOW_THRESHOLD&filterAiReviewInLang=cs").assert.containsExactly("ui-key")
    keyNames("filterAiReviewOutcome=AUTO_REVIEWED&filterAiReviewInLang=cs&filterAiReviewInLang=fr")
      .assert
      .isEmpty()
  }

  @Test
  fun `ignores results for an older text and below threshold results of reviewed translations`() {
    keyNames("filterAiReviewOutcome=BELOW_THRESHOLD&filterAiReviewInLang=de").assert.isEmpty()
    keyNames("filterAiReviewOutcome=BELOW_THRESHOLD&filterAiReviewInLang=fr").assert.isEmpty()
  }

  @Test
  fun `not reviewed yet needs an enabled rule that reviews the language and no current result`() {
    keyNames("filterAiReviewOutcome=NOT_REVIEWED").assert.containsExactly("ui-new", "ui-pending", "ui-stale")
    keyNames("filterAiReviewOutcome=NOT_REVIEWED&filterAiReviewOutcome=FAILED")
      .assert
      .containsExactly("plain-key", "ui-new", "ui-pending", "ui-stale")
  }

  @Test
  fun `not reviewed yet ignores rules that exclude the viewed branch`() {
    executeInNewTransaction(platformTransactionManager) {
      aiReviewRuleRepository.findAllByProjectId(projectId).forEach {
        it.branchNotIn = mutableListOf("main")
        aiReviewRuleRepository.save(it)
      }
    }
    keyNames("filterAiReviewOutcome=NOT_REVIEWED").assert.isEmpty()
  }

  @Test
  fun `is combined with the other translation filters with AND`() {
    keyNames("filterAiReviewOutcome=AUTO_REVIEWED&filterState=de,REVIEWED").assert.containsExactly("ui-key")
    keyNames("filterAiReviewOutcome=AUTO_REVIEWED&filterState=de,TRANSLATED").assert.isEmpty()
    keyNames("filterAiReviewOutcome=AUTO_REVIEWED&filterTag=legal").assert.isEmpty()
  }

  @Test
  fun `select all uses the AI review filters`() {
    val url = "/v2/projects/$projectId/translations/select-all?languages=de,cs&filterAiReviewOutcome=FAILED"
    val json = jsonMapper.readTree(performAuthGet(url).andIsOk.andGetContentAsString)
    json
      .path("ids")
      .items()
      .map { it.asLong() }
      .assert
      .containsExactly(data.plainKey.self.id)
  }

  @Test
  fun `is ignored when the feature flag is off`() {
    val all = keyNames("")
    tolgeeProperties.featureFlags.enabledForAll = emptyList()
    keyNames("filterAiReviewOutcome=FAILED").assert.isEqualTo(all)
  }

  @Test
  fun `is ignored without the AI review plan feature`() {
    val all = keyNames("")
    enabledFeaturesProvider.forceEnabled = setOf()
    keyNames("filterAiReviewOutcome=FAILED").assert.isEqualTo(all)
  }

  private fun JsonNode.items(): List<JsonNode> = (0 until size()).map { get(it) }
}
