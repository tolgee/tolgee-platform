package io.tolgee.model.activity

import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.PropertyModification
import io.tolgee.activity.data.RevisionType
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
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
  val entityId: Long
) : Serializable {

  /**
   * Map of field to object containing old and new values
   */
  @JdbcTypeCode(SqlTypes.JSON)
  var modifications: MutableMap<String, PropertyModification> = mutableMapOf()

  /**
   * Data, which are discribing the entity, but are not modified by the change
   */
  @JdbcTypeCode(SqlTypes.JSON)
  var describingData: Map<String, Any?>? = null

  /**
   * Relations describing the entity.
   * e.g. For translation, we would also need key and language data
   */
  @JdbcTypeCode(SqlTypes.JSON)
  var describingRelations: Map<String, EntityDescriptionRef>? = null

  @Enumerated
  lateinit var revisionType: RevisionType
}
