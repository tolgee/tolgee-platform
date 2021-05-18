package io.tolgee.model

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.springframework.data.util.ProxyUtils
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class StandardAuditModel : AuditModel() {
    @Id
    @GenericGenerator(
            name = "sequenceGenerator",
            strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
            parameters = [
                Parameter(name = "sequence_name", value = "hibernate_sequence"),
                Parameter(name = "optimizer", value = "pooled"),
                Parameter(name = "initial_value", value = "1000000000"),
                Parameter(name = "increment_size", value = "100")
            ]
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sequenceGenerator"
    )
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
