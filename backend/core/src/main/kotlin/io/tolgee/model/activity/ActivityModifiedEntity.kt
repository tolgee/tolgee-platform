package io.tolgee.model.activity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.PropertyModification
import io.tolgee.activity.data.RevisionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.Type
import java.io.Serializable

/**
 * Entity which is modified by the activity.
 */
@Entity
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
  val entityId: Long,
) : Serializable {
  /**
   * Map of field to object containing old and new values
   */
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var modifications: MutableMap<String, PropertyModification> = mutableMapOf()

  /**
   * Data, which are discribing the entity, but are not modified by the change
   */
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var describingData: Map<String, Any?>? = null

  /**
   * Relations describing the entity.
   * e.g. For translation, we would also need key and language data
   */
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var describingRelations: Map<String, EntityDescriptionRef>? = null

  @Enumerated
  var revisionType: RevisionType = RevisionType.MOD
}
