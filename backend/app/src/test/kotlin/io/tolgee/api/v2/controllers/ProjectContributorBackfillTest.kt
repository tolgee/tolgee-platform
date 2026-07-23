package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp

class ProjectContributorBackfillTest : AuthorizedControllerTest() {
  lateinit var testData: BaseTestData

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @BeforeEach
  fun setup() {
    testData = BaseTestData("backfill_author", "backfill_project")
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `backfill excludes null-typed revisions with no modified entities`() {
    val projectId = testData.project.id
    val userId = testData.user.id
    val t1 = Timestamp(1_600_000_000_000)
    val t2 = Timestamp(1_600_000_000_000 + 100_000)
    val t3 = Timestamp(1_600_000_000_000 + 200_000)

    insertRevision(projectId, userId, "COMPLEX_EDIT", t1)
    val nullTypedWithEntityId = insertRevision(projectId, userId, null, t2)
    insertModifiedEntity(nullTypedWithEntityId)
    insertRevision(projectId, userId, null, t3)

    jdbcTemplate.execute("DELETE FROM project_contributor")

    val backfillSql =
      ClassPathResource("db/changelog/projectContributorBackfill.sql")
        .inputStream
        .reader()
        .readText()
    jdbcTemplate.execute(backfillSql)

    val rows =
      jdbcTemplate.queryForList(
        "select user_id, first_contribution_at, last_contribution_at from project_contributor where project_id = ?",
        projectId,
      )

    assertThat(rows).hasSize(1)
    val row = rows.single()
    assertThat(row["user_id"]).isEqualTo(userId)
    assertThat(row["first_contribution_at"]).isEqualTo(t1)
    assertThat(row["last_contribution_at"]).isEqualTo(t2)
  }

  private fun insertRevision(
    projectId: Long,
    userId: Long,
    type: String?,
    timestamp: Timestamp,
  ): Long {
    val id = jdbcTemplate.queryForObject("select nextval('activity_sequence')", Long::class.java)!!
    jdbcTemplate.update(
      "insert into activity_revision (id, project_id, author_id, type, \"timestamp\") values (?, ?, ?, ?, ?)",
      id,
      projectId,
      userId,
      type,
      timestamp,
    )
    return id
  }

  private fun insertModifiedEntity(revisionId: Long) {
    jdbcTemplate.update(
      "insert into activity_modified_entity " +
        "(activity_revision_id, entity_class, entity_id, revision_type, modifications) " +
        "values (?, ?, ?, ?, '{}'::jsonb)",
      revisionId,
      "Translation",
      1L,
      0,
    )
  }
}
