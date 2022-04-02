package io.tolgee.model.activity

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.PropertyModification
import io.tolgee.activity.RevisionType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.io.Serializable
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class ActivityModifiedEntity(
  @ManyToOne
  @Id
  val activityRevision: ActivityRevision,

  @Id
  val entityClass: String,

  @Id
  val entityId: Long
) : Serializable {

  @Type(type = "jsonb")
  var modifications: Map<String, PropertyModification> = mutableMapOf()

  @Enumerated
  lateinit var revisionType: RevisionType
}
