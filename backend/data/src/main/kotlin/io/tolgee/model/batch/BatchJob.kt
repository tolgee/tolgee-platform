package io.tolgee.model.batch

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.activity.ActivityRevision
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.Index
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
@Table(name = "tolgee_batch_job", indexes = [Index(columnList = "debouncingKey")])
class BatchJob : StandardAuditModel(), IBatchJob {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var project: Project

  @ManyToOne(fetch = FetchType.LAZY)
  var author: UserAccount? = null

  @Type(type = "jsonb")
  var target: List<Any> = listOf()

  var totalItems: Int = 0

  var totalChunks: Int = 0

  var chunkSize: Int = 0

  @Enumerated(STRING)
  override var status: BatchJobStatus = BatchJobStatus.PENDING

  @Enumerated(STRING)
  var type: BatchJobType = BatchJobType.PRE_TRANSLATE_BT_TM

  @OneToOne(mappedBy = "batchJob", fetch = FetchType.LAZY)
  var activityRevision: ActivityRevision? = null

  @Type(type = "jsonb")
  var params: Any? = null

  val chunkedTarget get() = chunkTarget(chunkSize, target)

  var maxPerJobConcurrency: Int = -1

  @Enumerated(STRING)
  var jobCharacter: JobCharacter = JobCharacter.FAST

  var hidden: Boolean = false

  val dto get() = BatchJobDto.fromEntity(this)

  val debounceDurationInMs: Long? = null

  val debounceMaxWaitTimeInMs: Long? = null

  @Type(type = "text")
  val debouncingKey: String? = null

  companion object {
    fun <T> chunkTarget(chunkSize: Int, target: List<T>): List<List<T>> =
      if (chunkSize == 0) listOf(target) else target.chunked(chunkSize)
  }
}
