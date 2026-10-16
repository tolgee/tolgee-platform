package io.tolgee.model.batch

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.TranslationTargetProductSpec
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.activity.ActivityRevision
import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.util.Date

@Entity
@Table(
  name = "tolgee_batch_job",
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "author_id"),
    Index(columnList = "debouncing_key"),
    Index(columnList = "created_at"),
  ],
)
class BatchJob :
  StandardAuditModel(),
  IBatchJob {
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  var project: Project? = null

  @ManyToOne(fetch = FetchType.LAZY)
  var author: UserAccount? = null

  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  @Basic(fetch = FetchType.LAZY)
  var target: List<Any> = listOf()

  /**
   * A single Postgres jsonb value is limited to 256 MB, which a materialized [target] of millions of items exceeds.
   */
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  @Basic(fetch = FetchType.LAZY)
  var targetProduct: TranslationTargetProductSpec? = null

  var totalItems: Int = 0

  var totalChunks: Int = 0

  var chunkSize: Int = 0

  @Enumerated(STRING)
  override var status: BatchJobStatus = BatchJobStatus.PENDING

  @Enumerated(STRING)
  var type: BatchJobType = BatchJobType.PRE_TRANSLATE_BT_TM

  @OneToOne(mappedBy = "batchJob", fetch = FetchType.LAZY)
  var activityRevision: ActivityRevision? = null

  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var params: Any? = null

  /**
   * The maximum number of coroutines among all tolgee instances (e.g. k8s pods). The default value is -1,
   * which means that the concurrency is not limited.
   * (check `BatchJobConcurrentLauncher#ExecutionQueueItem.trySetRunningState()` for more info).
   */
  var maxPerJobConcurrency: Int = -1

  @Enumerated(STRING)
  var jobCharacter: JobCharacter = JobCharacter.FAST

  var hidden: Boolean = false

  val dto get() = BatchJobDto.fromEntity(this)

  var debounceDurationInMs: Long? = null

  var debounceMaxWaitTimeInMs: Long? = null

  var lastDebouncingEvent: Date? = null

  var debouncingKey: String? = null

  companion object {
    fun chunkCount(
      chunkSize: Int,
      targetSize: Int,
    ): Int {
      if (chunkSize == 0) return 1
      return (targetSize + chunkSize - 1) / chunkSize
    }

    fun <T> getChunk(
      chunkSize: Int,
      target: List<T>,
      chunkNumber: Int,
    ): List<T> {
      if (chunkSize == 0) {
        if (chunkNumber != 0) throw IndexOutOfBoundsException("Chunk $chunkNumber does not exist")
        return target
      }
      val from = chunkNumber * chunkSize
      if (chunkNumber < 0 || from >= target.size) {
        throw IndexOutOfBoundsException("Chunk $chunkNumber does not exist")
      }
      return target.subList(from, minOf(from + chunkSize, target.size))
    }
  }
}
