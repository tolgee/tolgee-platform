package io.tolgee.model.batch

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(
  indexes = [
    javax.persistence.Index(columnList = "chunkNumber"),
    javax.persistence.Index(columnList = "status"),
  ]
)
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class BatchJobChunkExecution : StandardAuditModel() {
  @ManyToOne
  lateinit var batchJob: BatchJob

  @Enumerated(EnumType.STRING)
  var status: BatchJobChunkExecutionStatus = BatchJobChunkExecutionStatus.PENDING

  var chunkNumber: Int = 0

  @Type(type = "jsonb")
  var successTargets: List<Long> = listOf()

  @Column(columnDefinition = "text")
  var exception: String? = null

  var executeAfter: Date? = null

  @ColumnDefault("false")
  var retry: Boolean = false
}
