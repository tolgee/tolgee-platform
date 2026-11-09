package io.tolgee.ee.aiReview

import io.tolgee.AbstractSpringTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AiReviewColumnDefaultsTest : AbstractSpringTest() {
  @Test
  fun `existing rows get the documented defaults from the migration`() {
    columnDefault("project", "ai_approved_source_change_action").assert.contains("'RETRANSLATE'")
    columnDefault("project", "human_edited_source_change_action").assert.contains("'MARK_OUTDATED'")
    columnDefault("project", "human_reviewed_source_change_action").assert.contains("'MARK_OUTDATED'")
    columnDefault("prompt", "type").assert.contains("'TRANSLATE'")
    columnDefault("prompt", "revision").assert.isEqualTo("0")
    columnDefault("ai_review_rule", "auto_reviewed_count").assert.isEqualTo("0")
    columnDefault("ai_review_rule", "rejected_count").assert.isEqualTo("0")
  }

  private fun columnDefault(
    table: String,
    column: String,
  ): String =
    entityManager
      .createNativeQuery(
        """
        select column_default from information_schema.columns
        where table_schema = 'public' and table_name = :table and column_name = :column
        """,
      ).setParameter("table", table)
      .setParameter("column", column)
      .singleResult as String
}
