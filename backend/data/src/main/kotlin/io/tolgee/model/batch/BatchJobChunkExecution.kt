package io.tolgee.model.batch

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.constants.Message
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.activity.ActivityRevision
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(
  name = "tolgee_batch_job_chunk_execution",
  indexes = [
    javax.persistence.Index(columnList = "chunkNumber"),
    javax.persistence.Index(columnList = "status"),
  ]
)
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class BatchJobChunkExecution : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var batchJob: BatchJob

  @Enumerated(EnumType.STRING)
  var status: BatchJobChunkExecutionStatus = BatchJobChunkExecutionStatus.PENDING

  var chunkNumber: Int = 0

  @Type(type = "jsonb")
  var successTargets: List<Any> = listOf()

  @Column(columnDefinition = "text")
  var exception: String? = null

  @Enumerated(EnumType.STRING)
  var errorMessage: Message? = null

  var executeAfter: Date? = null

  @ColumnDefault("false")
  var retry: Boolean = false

  @OneToOne(fetch = FetchType.LAZY, mappedBy = "batchJobChunkExecution")
  var activityRevision: ActivityRevision? = null
}
