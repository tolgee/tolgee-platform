package io.tolgee.model

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.SequenceGenerator
import org.springframework.data.util.ProxyUtils

@MappedSuperclass
abstract class StandardAuditModel : AuditModel(), EntityWithId {
  @Id
  @SequenceGenerator(
    name = "sequenceGenerator",
    sequenceName = "hibernate_sequence",
    initialValue = 1000000000,
    allocationSize = 1000
  )
  @GeneratedValue(
    strategy = GenerationType.SEQUENCE,
    generator = "sequenceGenerator"
  )
  override var id: Long = 0

  @Transient
  override var disableActivityLogging = false

  override fun equals(other: Any?): Boolean {
    other ?: return false

    if (this === other) return true

    if (javaClass != ProxyUtils.getUserClass(other)) return false

    other as StandardAuditModel

    // entity is not stored yet, so ID can be null for different entities
    if (this.id == 0L && other.id == 0L) {
      return false
    }

    return this.id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun toString() = "${this.javaClass.name}(id: $id)"
}
