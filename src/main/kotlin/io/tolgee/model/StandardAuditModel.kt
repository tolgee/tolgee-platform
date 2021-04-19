package io.tolgee.model

import org.springframework.data.util.ProxyUtils
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class StandardAuditModel : AuditModel() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    override fun equals(other: Any?): Boolean {
        other ?: return false

        if (this === other) return true

        if (javaClass != ProxyUtils.getUserClass(other)) return false

        other as StandardAuditModel

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return 31
    }

    override fun toString() = "${this.javaClass.name}(id: $id)"
}
