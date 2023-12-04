package io.tolgee.model.activity

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.RevisionType
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import org.springframework.data.annotation.AccessType
import java.io.Serializable
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.ManyToOne

@Entity
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
@IdClass(ActivityDescribingEntityId::class)
class ActivityDescribingEntity(
  @ManyToOne
  @Id
  @AccessType(AccessType.Type.PROPERTY)
  @NotFound(action = NotFoundAction.IGNORE)
  val activityRevision: ActivityRevision,

  @Id
  @AccessType(AccessType.Type.PROPERTY)
  val entityClass: String,

  @Id
  @AccessType(AccessType.Type.PROPERTY)
  val entityId: Long
) : Serializable {

  @Type(type = "jsonb")
  var data: Map<String, Any?> = mutableMapOf()

  @Type(type = "jsonb")
  var describingRelations: Map<String, EntityDescriptionRef>? = null

  @Enumerated
  lateinit var revisionType: RevisionType
}
