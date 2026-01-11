package io.tolgee.model.keyBigMeta

import io.tolgee.model.AuditModel
import io.tolgee.model.Project
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.domain.Persistable

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "key1Id, key2Id", unique = true),
    Index(columnList = "key1Id"),
    Index(columnList = "key2Id"),
  ],
)
@IdClass(KeysDistanceId::class)
class KeysDistance(
  @Id
  var key1Id: Long = 0,
  @Id
  var key2Id: Long = 0,
) : AuditModel(),
  Persistable<KeysDistanceId> {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var project: Project

  // TODO: Remove this
  @Deprecated("Kept for backward compatibility. Can be removed in any next path version.")
  @ColumnDefault("10000")
  var score: Long = 10000

  var distance: Double = 0.0

  var hits: Long = 1

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as KeysDistance

    if (key1Id != other.key1Id) return false
    if (key2Id != other.key2Id) return false
    if (distance != other.distance) return false
    return hits == other.hits
  }

  override fun hashCode(): Int {
    var result = key1Id.hashCode()
    result = 31 * result + key2Id.hashCode()
    result = 31 * result + distance.hashCode()
    result = 31 * result + hits.hashCode()
    return result
  }

  override fun getId(): KeysDistanceId? {
    return KeysDistanceId(key1Id, key2Id)
  }

  override fun isNew(): Boolean {
    return new
  }

  @Transient
  var new = false
}
