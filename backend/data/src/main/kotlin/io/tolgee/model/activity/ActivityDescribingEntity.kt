package io.tolgee.model.activity

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.EntityDescriptionRef
import io.tolgee.activity.RevisionType
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
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
  @NotFound(action = NotFoundAction.IGNORE)
  val activityRevision: ActivityRevision,

  @Id
  val entityClass: String,

  @Id
  val entityId: Long
) : Serializable {

  @Type(type = "jsonb")
  var data: Map<String, Any?> = mutableMapOf()

  @Type(type = "jsonb")
  var describingRelations: Map<String, EntityDescriptionRef>? = null

  @Enumerated
  lateinit var revisionType: RevisionType
}
