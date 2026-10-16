package io.tolgee.ee.aiReview

import io.tolgee.ee.data.aiReview.AiReviewRunRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.util.Logging
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.logger
import org.hibernate.resource.jdbc.spi.StatementInspector
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Diagnostic: builds a project with [KEY_COUNT] keys in 3 languages and prints timings and generic
 * query plans of the AI review filters, the run preview and the rule backlog. Run manually:
 *
 * ```
 * TOLGEE_AI_REVIEW_PERF_TEST=true ./gradlew :ee-test:test --tests "io.tolgee.ee.aiReview.AiReviewFilterPerfTest"
 * ```
 */
@EnabledIfEnvironmentVariable(named = "TOLGEE_AI_REVIEW_PERF_TEST", matches = "true")
@TestPropertySource(
  properties = [
    "spring.jpa.properties.hibernate.session_factory.statement_inspector=" +
      "io.tolgee.ee.aiReview.AiReviewFilterPerfTest\$RecordingInspector",
  ],
)
class AiReviewFilterPerfTest :
  AbstractAiReviewTest(),
  Logging {
  class RecordingInspector : StatementInspector {
    override fun inspect(sql: String): String {
      if (sql.contains("translation_ai_review")) statements.add(sql)
      return sql
    }
  }

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Test
  fun `measures the AI review filters on a large project`() {
    val rule = testData.addRule { tagIn = mutableListOf("ui") }
    testData.addRule { tagIn = mutableListOf("legal") }.neverForLanguage(testData.french.self)
    testData.projectBuilder.apply {
      (1..KEY_COUNT).forEach { i ->
        addKey { name = "perf-%06d".format(i) }.build {
          addTag(listOf("ui", "legal", "other")[i % 3])
          addTranslation("en", "Source $i")
          listOf("de", "cs", "fr").forEach { tag ->
            val translation = addTranslation(tag, "Text $i $tag")
            if (i % 4 == 0) {
              translation.self.state = TranslationState.REVIEWED
              translation.addAiReview {
                outcome = AiReviewOutcome.AUTO_REVIEWED
                this.rule = rule.self
                textHash = aiReviewTextHash(translation.self.text)
                score = 90
                threshold = 85
              }
            }
          }
        }
      }
    }
    saveTestData()

    listOf(
      "filterAiReviewOutcome=NOT_REVIEWED",
      "filterAiReviewOutcome=AUTO_REVIEWED",
      "filterAiReviewOutcome=BELOW_THRESHOLD&filterAiReviewOutcome=FAILED",
    ).forEach { query ->
      statements.clear()
      val url = "/v2/projects/$projectId/translations?languages=en,de,cs,fr&size=20&$query"
      performAuthGet(url).andIsOk
      val ms = measure { performAuthGet(url).andIsOk }
      logger.info(">>> $query: ${ms}ms")
      statements.distinct().forEach { explain(it) }
    }

    val keyIds =
      testData.projectBuilder.data.keys
        .map { it.self.id }
    val previewMs =
      measure {
        performAuthPost(
          "/v2/projects/$projectId/ai-review/run/preview",
          AiReviewRunRequest(keyIds = keyIds),
        ).andIsOk
      }
    logger.info(">>> preview for ${keyIds.size} keys: ${previewMs}ms")
    val backlogMs = measure { performAuthGet("/v2/projects/$projectId/ai-review/rules/backlog").andIsOk }
    logger.info(">>> rule backlog: ${backlogMs}ms")
  }

  private fun explain(sql: String) {
    var index = 0
    val numbered = sql.replace(Regex("\\?")) { "$${++index}" }
    val plan =
      runCatching { jdbcTemplate.queryForList("EXPLAIN (GENERIC_PLAN) $numbered", String::class.java) }
        .fold({ it.joinToString("\n") }, { "explain failed: ${it.message}" })
    logger.info(">>> SQL: $sql\n>>> PLAN:\n$plan")
  }

  private fun measure(block: () -> Unit): Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
  }

  companion object {
    private const val KEY_COUNT = 20_000
    val statements = CopyOnWriteArrayList<String>()
  }
}
