package io.tolgee.model.batch

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Entity
import javax.persistence.OneToOne

@Entity
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class CopyTranslationJobParams : StandardAuditModel() {
  @OneToOne(optional = false)
  lateinit var batchJob: BatchJob

  var sourceLanguageId: Long = 0

  @Type(type = "jsonb")
  var targetLanguageIds: List<Long> = listOf()
}
