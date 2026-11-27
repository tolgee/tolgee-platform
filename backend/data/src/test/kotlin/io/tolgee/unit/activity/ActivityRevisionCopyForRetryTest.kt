package io.tolgee.unit.activity

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.enums.SystemActor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

class ActivityRevisionCopyForRetryTest {
  private val carried =
    setOf("authorId", "type", "systemActor", "projectId", "organizationId", "batchJobChunkExecution")
  private val startsFresh =
    setOf(
      "timestamp",
      "meta",
      "describingRelations",
      "modifiedEntities",
      "batchJob",
      "isInitializedByInterceptor",
      "cancelledBatchJobExecutionCount",
    )

  @Test
  fun `every field is either carried to the next attempt or starts fresh`() {
    val settable = ActivityRevision::class.memberProperties.filterIsInstance<KMutableProperty1<ActivityRevision, *>>()
    assertThat(settable.map { it.name }).containsExactlyInAnyOrderElementsOf(carried + startsFresh)

    val revision =
      ActivityRevision().apply {
        authorId = 1
        type = ActivityType.SET_TRANSLATIONS
        systemActor = SystemActor.AI_REVIEWER
        projectId = 2
        organizationId = 3
        batchJobChunkExecution = BatchJobChunkExecution()
        cancelledBatchJobExecutionCount = 4
      }
    val copy = revision.copyForRetry()

    assertThat(copy.cancelledBatchJobExecutionCount).isNull()
    assertThat(settable.filter { it.name in carried }).allSatisfy {
      assertThat(it.get(revision)).describedAs(it.name).isNotNull()
      assertThat(it.get(copy)).describedAs(it.name).isEqualTo(it.get(revision))
    }
  }
}
