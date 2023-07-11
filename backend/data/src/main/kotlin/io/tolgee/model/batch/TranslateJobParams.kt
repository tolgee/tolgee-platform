package io.tolgee.model.batch

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.constants.MtServiceType
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
class TranslateJobParams : StandardAuditModel() {
  @OneToOne(optional = false)
  lateinit var batchJob: BatchJob

  @Type(type = "jsonb")
  var targetLanguageIds: List<Long> = mutableListOf()

  var useMachineTranslation: Boolean = true

  var useTranslationMemory: Boolean = true

  var service: MtServiceType? = null
}
