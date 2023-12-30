package io.tolgee.model.activity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.data.EntityDescriptionRef
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.Type
import java.io.Serializable

@Entity
@IdClass(ActivityDescribingEntityId::class)
class ActivityDescribingEntity(
  @ManyToOne
  @Id
  @NotFound(action = NotFoundAction.IGNORE)
  val activityRevision: ActivityRevision,
  @Id
  val entityClass: String,
  @Id
  val entityId: Long,
) : Serializable {
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var data: Map<String, Any?> = mutableMapOf()

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  var describingRelations: Map<String, EntityDescriptionRef>? = null
}
