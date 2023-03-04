package io.tolgee.ee.model

import com.vladmihalcea.hibernate.type.array.EnumArrayType
import io.tolgee.constants.Feature
import io.tolgee.model.AuditModel
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity()
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
@Table(schema = "ee")
class EeSubscription : AuditModel() {
  @field:Id
  val id: Int = 0

  @field:NotBlank
  lateinit var licenseKey: String

  @field:NotNull
  var currentPeriodEnd: Date? = null

  var cancelAtPeriodEnd: Boolean = false

  var userLimit: Long = 0L

  @Type(type = "enum-array")
  @Column(name = "enabled_features", columnDefinition = "varchar[]")
  var enabledFeatures: Array<Feature> = arrayOf()
}
