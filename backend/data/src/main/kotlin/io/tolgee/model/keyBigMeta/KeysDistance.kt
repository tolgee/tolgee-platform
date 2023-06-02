package io.tolgee.model.keyBigMeta

import io.tolgee.model.AuditModel
import io.tolgee.model.Project
import org.springframework.data.domain.Persistable
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.Index
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(
  indexes = [
    Index(columnList = "key1Id, key2Id", unique = true),
  ]
)
@IdClass(KeysDistanceId::class)
class KeysDistance(
  @Id
  var key1Id: Long = 0,

  @Id
  var key2Id: Long = 0
) : AuditModel(), Persistable<KeysDistanceId> {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var project: Project

  var distance: Long = 10000

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
