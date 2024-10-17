package io.tolgee.model.batch

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.constants.Message
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.activity.ActivityRevision
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type
import java.util.*

@Entity
@Table(
  name = "tolgee_batch_job_chunk_execution",
  indexes = [
    Index(columnList = "batch_job_id"),
    Index(columnList = "chunkNumber"),
    Index(columnList = "status"),
  ],
)
class BatchJobChunkExecution : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var batchJob: BatchJob

  @Enumerated(EnumType.STRING)
  var status: BatchJobChunkExecutionStatus = BatchJobChunkExecutionStatus.PENDING

  var chunkNumber: Int = 0

  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var successTargets: List<Any> = listOf()

  @Column(columnDefinition = "text")
  var stackTrace: String? = null

  /**
   * This is used to count allowed retries
   *
   * When max retries for this error keys are reached, the chunk will be marked as failed
   */
  @Column(columnDefinition = "text")
  var errorKey: String? = null

  @Enumerated(EnumType.STRING)
  var errorMessage: Message? = null

  var executeAfter: Date? = null

  @ColumnDefault("false")
  var retry: Boolean = false

  @OneToOne(fetch = FetchType.LAZY, mappedBy = "batchJobChunkExecution")
  var activityRevision: ActivityRevision? = null
}
