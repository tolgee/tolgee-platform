package io.tolgee.model.activity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.data.DescribingDataMap
import io.tolgee.activity.data.DescribingRelationsMap
import io.tolgee.activity.data.PropertyModifications
import io.tolgee.activity.data.RevisionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Type
import java.io.Serializable

/** Insert-only record of one entity touched by an activity. */
@Entity
@Immutable
@Table(
  indexes = [
    Index(columnList = "activity_revision_id,branch_id"),
  ],
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
  val entityId: Long,
) : Serializable {
  @Column(name = "branch_id")
  var branchId: Long? = null

  /** Field-name → (old, new) map of modifications captured for this entity. */
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var modifications: PropertyModifications = PropertyModifications(entityClass)

  /** Scalar data describing the entity, but not modified by the change. */
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var describingData: DescribingDataMap? = null

  /** Related-entity refs describing the entity (e.g. a Translation's key and language). */
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var describingRelations: DescribingRelationsMap? = null

  @Enumerated
  var revisionType: RevisionType = RevisionType.MOD
}
