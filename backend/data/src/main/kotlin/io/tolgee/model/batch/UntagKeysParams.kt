package io.tolgee.model.batch

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Entity
import javax.persistence.OneToOne
import javax.validation.constraints.NotEmpty

@Entity
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class UntagKeysParams : StandardAuditModel() {
  @OneToOne(optional = false)
  lateinit var batchJob: BatchJob

  @Type(type = "jsonb")
  @NotEmpty
  var tags: List<String> = listOf()
}
