package io.tolgee.ee.aiReview

import io.tolgee.development.testDataBuilder.data.AiReviewRuleParityTestData
import io.tolgee.ee.data.aiReview.AiReviewRunRequest
import io.tolgee.ee.service.aiReview.AiReviewRuleMatcher
import io.tolgee.ee.service.aiReview.AiReviewTriggerService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TranslationState
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.testing.assert
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.aiReviewTextHashSql
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.ObjectMapper

class AiReviewRuleParityTest : AbstractAiReviewTest() {
  @Autowired
  lateinit var jsonMapper: ObjectMapper

  @Autowired
  lateinit var triggerService: AiReviewTriggerService

  @Autowired
  lateinit var aiReviewRuleRepository: AiReviewRuleRepository

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  override fun createTestData() = AiReviewRuleParityTestData()

  private val data get() = testData as AiReviewRuleParityTestData

  private val nonBaseLanguages get() = listOf(data.german, data.czech, data.french).map { it.self }

  @BeforeEach
  fun save() {
    saveTestData()
  }

  @Test
  fun `not reviewed yet filter selects what the rule matcher would review`() {
    val expected = expectedReviewedKeyNamesByLanguage()
    expected.values
      .flatten()
      .assert
      .isNotEmpty
    nonBaseLanguages.forEach { language ->
      notReviewedKeyNames(language.tag)
        .assert
        .describedAs("language ${language.tag}")
        .containsExactlyInAnyOrderElementsOf(expected[language.tag].orEmpty())
    }
  }

  @Test
  fun `preview reviews exactly what the not reviewed yet filter shows`() {
    val filtered = nonBaseLanguages.sumOf { notReviewedKeyNames(it.tag).size }
    val allKeyIds =
      data.projectBuilder.data.keys
        .map { it.self.id }
    performAuthPost("/v2/projects/$projectId/ai-review/run/preview", AiReviewRunRequest(keyIds = allKeyIds))
      .andIsOk
      .andAssertThatJson {
        node("withoutRereview.willReview").isEqualTo(filtered)
      }
  }

  @Test
  fun `rule backlog equals the not reviewed yet filter for that rule`() {
    val rules = listOf(data.uiOrUntaggedRule, data.taggedOutsideAdminRule, data.checkoutDryRunRule)
    val json =
      jsonMapper.readTree(
        performAuthGet("/v2/projects/$projectId/ai-review/rules/backlog").andIsOk.andGetContentAsString,
      )
    val backlogs =
      (0 until json.path("backlogs").size()).associate {
        val backlog = json.path("backlogs").get(it)
        backlog.path("ruleId").asLong() to backlog.path("notReviewed").asLong()
      }
    rules.forEach { rule ->
      val filtered =
        nonBaseLanguages.sumOf { notReviewedKeyNames(it.tag, "&filterAiReviewRuleId=${rule.self.id}").size }
      backlogs[rule.self.id]
        .assert
        .describedAs(rule.self.name)
        .isEqualTo(filtered.toLong())
    }
  }

  @Test
  fun `sql text hash equals the stored hash for non-ascii text`() {
    val text = data.autoReviewedNonAscii.self.text
    jdbcTemplate
      .queryForObject("SELECT ${aiReviewTextHashSql("?")}", String::class.java, text)
      .assert
      .isEqualTo(aiReviewTextHash(text))
    keyNames("filterAiReviewOutcome=AUTO_REVIEWED&filterAiReviewInLang=de").assert.containsExactly("hash-check")
  }

  private fun expectedReviewedKeyNamesByLanguage(): Map<String, List<String>> {
    val rules =
      executeInNewTransaction(platformTransactionManager) {
        aiReviewRuleRepository.findAllEnabledByProjectId(projectId).onEach {
          it.languageIds.size
          it.languageThresholds.size
        }
      }
    val keys = data.projectBuilder.data.keys
    val scopes = triggerService.getKeyScopes(keys.map { it.self.id })
    val nonBaseLanguageIds = nonBaseLanguages.map { it.id }.toSet()
    return keys
      .flatMap { key -> key.translations.map { key to it.self } }
      .filter { (_, translation) ->
        translation.language.id in nonBaseLanguageIds &&
          translation.state == TranslationState.TRANSLATED &&
          !translation.text.isNullOrBlank()
      }.filter { (key, translation) ->
        val scope = scopes[key.self.id] ?: AiReviewRuleMatcher.KeyScope(emptySet(), null)
        AiReviewRuleMatcher.match(rules, scope, translation.language.id) is AiReviewRuleMatcher.Match.Review
      }.groupBy({ (_, translation) -> translation.language.tag }, { (key, _) -> key.self.name })
  }

  private fun notReviewedKeyNames(
    languageTag: String,
    extra: String = "",
  ): List<String> = keyNames("filterAiReviewOutcome=NOT_REVIEWED&filterAiReviewInLang=$languageTag$extra")

  private fun keyNames(query: String): List<String> {
    val url = "/v2/projects/$projectId/translations?languages=en,de,cs,fr&sort=keyName&size=1000&$query"
    val keys = jsonMapper.readTree(performAuthGet(url).andIsOk.andGetContentAsString).path("_embedded").path("keys")
    return (0 until keys.size()).map { keys.get(it).path("keyName").asString() }
  }
}
