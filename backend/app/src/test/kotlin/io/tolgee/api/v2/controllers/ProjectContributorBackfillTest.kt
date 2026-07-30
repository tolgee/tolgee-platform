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
  fun `backfill counts every revision with a project and an author, whatever its type`() {
    val projectId = testData.project.id
    val userId = testData.user.id
    val t1 = Timestamp(1_600_000_000_000)
    val t2 = Timestamp(1_600_000_000_000 + 100_000)
    val t3 = Timestamp(1_600_000_000_000 + 200_000)

    insertRevision(projectId, userId, "COMPLEX_EDIT", t1)
    val nullTypedWithEntityId = insertRevision(projectId, userId, null, t2)
    insertModifiedEntity(nullTypedWithEntityId)
    insertRevision(projectId, userId, null, t3)

    jdbcTemplate.update("DELETE FROM project_contributor WHERE project_id = ?", projectId)

    jdbcTemplate.execute(backfillSql())

    val rows =
      jdbcTemplate.queryForList(
        "select user_id, first_contribution_at, last_contribution_at from project_contributor where project_id = ?",
        projectId,
      )

    assertThat(rows).hasSize(1)
    val row = rows.single()
    assertThat(row["user_id"]).isEqualTo(userId)
    assertThat(row["first_contribution_at"]).isEqualTo(t1)
    assertThat(row["last_contribution_at"]).isEqualTo(t3)
  }

  @Test
  fun `the trigger and the backfill agree - both record every revision with a project and an author`() {
    val projectId = testData.project.id
    val userId = testData.user.id
    jdbcTemplate.update("DELETE FROM project_contributor WHERE project_id = ?", projectId)

    insertRevision(projectId, userId, null, Timestamp(1_600_000_000_000))
    insertRevision(projectId, userId, "COMPLEX_EDIT", Timestamp(1_600_000_200_000))
    val fromTrigger = contributorRows(projectId)

    jdbcTemplate.update("DELETE FROM project_contributor WHERE project_id = ?", projectId)
    jdbcTemplate.execute(backfillSql())

    assertThat(fromTrigger).hasSize(1)
    assertThat(fromTrigger).isEqualTo(contributorRows(projectId))
  }

  @Test
  fun `backfill widens an existing narrower project_contributor row via the conflict merge`() {
    val projectId = testData.project.id
    val userId = testData.user.id
    val t1 = Timestamp(1_600_000_000_000)
    val t2 = Timestamp(1_600_000_000_000 + 100_000)
    val t3 = Timestamp(1_600_000_000_000 + 200_000)

    insertRevision(projectId, userId, "COMPLEX_EDIT", t1)
    insertRevision(projectId, userId, "COMPLEX_EDIT", t3)

    jdbcTemplate.update("DELETE FROM project_contributor WHERE project_id = ?", projectId)
    jdbcTemplate.update(
      "insert into project_contributor (project_id, user_id, first_contribution_at, last_contribution_at) " +
        "values (?, ?, ?, ?)",
      projectId,
      userId,
      t2,
      t2,
    )

    jdbcTemplate.execute(backfillSql())

    val row =
      jdbcTemplate
        .queryForList(
          "select first_contribution_at, last_contribution_at from project_contributor where project_id = ? and user_id = ?",
          projectId,
          userId,
        ).single()
    assertThat(row["first_contribution_at"]).isEqualTo(t1)
    assertThat(row["last_contribution_at"]).isEqualTo(t3)
  }

  @Test
  fun `backfill skips revisions with null project_id or null author_id`() {
    val projectId = testData.project.id
    val userId = testData.user.id
    val t = Timestamp(1_600_000_000_000)

    insertRevision(projectId, userId, "COMPLEX_EDIT", t)
    insertRevision(null, userId, "COMPLEX_EDIT", t)
    insertRevision(projectId, null, "COMPLEX_EDIT", t)

    jdbcTemplate.update("DELETE FROM project_contributor WHERE project_id = ?", projectId)
    jdbcTemplate.execute(backfillSql())

    val rows =
      jdbcTemplate.queryForList("select project_id, user_id from project_contributor where project_id = ?", projectId)
    assertThat(rows).hasSize(1)
    assertThat(rows.single()["project_id"]).isEqualTo(projectId)
    assertThat(rows.single()["user_id"]).isEqualTo(userId)
  }

  @Test
  fun `the trigger skips an activity_revision with null project_id`() {
    val userId = testData.user.id
    insertRevision(null, userId, "COMPLEX_EDIT", Timestamp(1_600_000_000_000))

    val count =
      jdbcTemplate.queryForObject(
        "select count(*) from project_contributor where user_id = ?",
        Long::class.java,
        userId,
      )
    assertThat(count).isEqualTo(0)
  }

  private fun contributorRows(projectId: Long) =
    jdbcTemplate.queryForList(
      "select user_id, first_contribution_at, last_contribution_at from project_contributor where project_id = ?",
      projectId,
    )

  private fun insertRevision(
    projectId: Long?,
    userId: Long?,
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

  private fun backfillSql(): String =
    ClassPathResource("db/changelog/projectContributorBackfill.sql")
      .inputStream
      .reader()
      .readText()

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
