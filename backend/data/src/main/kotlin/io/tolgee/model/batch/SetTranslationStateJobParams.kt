package io.tolgee.model.batch

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.TranslationState
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Entity
import javax.persistence.OneToOne

@Entity
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class SetTranslationStateJobParams : StandardAuditModel() {
  @OneToOne(optional = false)
  lateinit var batchJob: BatchJob

  @Type(type = "jsonb")
  var languageIds: List<Long> = listOf()

  var state: TranslationState? = null
}
