package io.tolgee.ee.model

import com.vladmihalcea.hibernate.type.array.EnumArrayType
import io.tolgee.constants.Feature
import io.tolgee.ee.data.SubscriptionStatus
import io.tolgee.model.AuditModel
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotBlank
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
@Table(schema = "ee")
class EeSubscription : AuditModel() {
  @field:Id
  val id: Int = 1

  @field:NotBlank
  lateinit var licenseKey: String

  @field:ColumnDefault("Plan")
  lateinit var name: String

  @field:NotNull
  var currentPeriodEnd: Date? = null

  var cancelAtPeriodEnd: Boolean = false

  @Type(type = "enum-array")
  @Column(name = "enabled_features", columnDefinition = "varchar[]")
  var enabledFeatures: Array<Feature> = arrayOf()
    get() {
      return if (status != SubscriptionStatus.ERROR && status != SubscriptionStatus.CANCELED) field else arrayOf()
    }

  @Enumerated(EnumType.STRING)
  @ColumnDefault("ACTIVE")
  var status: SubscriptionStatus = SubscriptionStatus.ACTIVE

  var lastValidCheck: Date? = null
}
