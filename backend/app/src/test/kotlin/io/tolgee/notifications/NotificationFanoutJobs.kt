package io.tolgee.notifications

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import org.springframework.jdbc.core.JdbcTemplate

class NotificationFanoutJobs(
  private val jdbcTemplate: JdbcTemplate,
) {
  data class FanoutJob(
    val id: Long,
    val status: String,
    val target: String,
    val hidden: Boolean,
    val authorId: Long?,
  )

  fun ofProject(projectId: Long): List<FanoutJob> =
    jdbcTemplate.query(
      """
      select id, status, target::text, hidden, author_id from tolgee_batch_job
      where type = 'NOTIFICATION_FANOUT' and project_id = ? order by id
      """,
      { rs, _ ->
        FanoutJob(
          id = rs.getLong(1),
          status = rs.getString(2),
          target = rs.getString(3),
          hidden = rs.getBoolean(4),
          authorId = rs.getLong(5).takeUnless { rs.wasNull() },
        )
      },
      projectId,
    )

  fun waitForAllSucceeded(
    projectId: Long,
    atLeast: Int = 1,
  ): List<FanoutJob> {
    waitFor(timeout = 30_000, pollTime = 100) {
      val jobs = ofProject(projectId)
      jobs.size >= atLeast && jobs.all { it.status == "SUCCESS" }
    }
    return ofProject(projectId)
  }

  fun entityCount(notificationId: Long) =
    jdbcTemplate.queryForObject(
      "select count(*) from notification_entity where notification_id = ?",
      Int::class.java,
      notificationId,
    )

  fun revisionIdsOf(notificationId: Long): List<Long> =
    jdbcTemplate
      .queryForList(
        "select unnest(activity_revision_ids) from notification where id = ?",
        Long::class.java,
        notificationId,
      ).filterNotNull()

  fun latestRevisionId(projectId: Long) =
    jdbcTemplate.queryForObject(
      "select max(id) from activity_revision where project_id = ?",
      Long::class.java,
      projectId,
    )!!
}

fun ProjectAuthControllerTest.finishSetStateBatchJob(
  batchJobService: BatchJobService,
  keyId: Long,
  languageIds: List<Long>,
): Long {
  val response =
    performProjectAuthPost(
      "start-batch-job/set-translation-state",
      mapOf("keyIds" to listOf(keyId), "languageIds" to languageIds, "state" to "REVIEWED"),
    ).andIsOk
  val jobId = (objectMapper.readValue(response.andGetContentAsString, Map::class.java)["id"] as Number).toLong()
  waitFor(pollTime = 100) { batchJobService.findJobDto(jobId)?.status?.completed == true }
  return jobId
}
