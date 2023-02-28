package io.tolgee.model

import com.vladmihalcea.hibernate.type.array.EnumArrayType
import io.tolgee.constants.Feature
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.validation.constraints.NotNull

@Entity
@TypeDef(
  name = "enum-array",
  typeClass = EnumArrayType::class,
  parameters = [
    Parameter(
      name = EnumArrayType.SQL_ARRAY_TYPE,
      value = "varchar"
    )
  ]
)
class EeSubscription : StandardAuditModel() {
  @field:NotNull
  lateinit var licenceKey: String

  @field:NotNull
  lateinit var paidUntil: Date

  @Type(type = "enum-array")
  @Column(name = "enabled_features", columnDefinition = "varchar[]")
  val enabledFeatures: Array<Feature> = arrayOf()
}
