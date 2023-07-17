package io.tolgee.model.batch

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.BatchJobType
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
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

@Entity
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class BatchJob : StandardAuditModel(), IBatchJob {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var project: Project

  @ManyToOne(fetch = FetchType.LAZY)
  var author: UserAccount? = null

  @Type(type = "jsonb")
  var target: List<Long> = listOf()

  var totalItems: Int = 0

  var totalChunks: Int = 0

  var chunkSize: Int = 0

  @Enumerated(STRING)
  override var status: BatchJobStatus = BatchJobStatus.PENDING

  @Enumerated(STRING)
  var type: BatchJobType = BatchJobType.AUTO_TRANSLATION

  @OneToOne(mappedBy = "batchJob", fetch = FetchType.LAZY)
  var activityRevision: ActivityRevision? = null

  val chunkedTarget get() = chunkTarget(chunkSize, target)

  val dto get() = BatchJobDto.fromEntity(this)

  companion object {
    fun chunkTarget(chunkSize: Int, target: List<Long>): List<List<Long>> =
      if (chunkSize == 0) listOf(target) else target.chunked(chunkSize)
  }
}
