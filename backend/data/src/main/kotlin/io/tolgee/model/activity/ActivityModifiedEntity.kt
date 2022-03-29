package io.tolgee.model.activity

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.PropertyModification
import io.tolgee.activity.data.RevisionType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.io.Serializable
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.ManyToOne

/**
 * Entity which is modified by the activity.
 */
@Entity
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
@IdClass(ActivityModifiedEntityId::class)
class ActivityModifiedEntity(
  @ManyToOne
  @Id
  val activityRevision: ActivityRevision,

  /**
   * Class of the modified entity
   * e.g. Translation, Key
   */
  @Id
  val entityClass: String,

  /**
   * ID of the modified entity
   */
  @Id
  val entityId: Long
) : Serializable {

  /**
   * Map of field to object containing old and new values
   */
  @Type(type = "jsonb")
  var modifications: MutableMap<String, PropertyModification> = mutableMapOf()

  /**
   * Data, which are discribing the entity, but are not modified by the change
   */
  @Type(type = "jsonb")
  var describingData: Map<String, Any?>? = null

  /**
   * Relations describing the entity.
   * e.g. For translation, we would also need key and language data
   */
  @Type(type = "jsonb")
  var describingRelations: Map<String, EntityDescriptionRef>? = null

  @Enumerated
  lateinit var revisionType: RevisionType
}
