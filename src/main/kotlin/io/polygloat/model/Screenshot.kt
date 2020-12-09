package io.polygloat.model

import org.apache.commons.codec.digest.DigestUtils
import javax.persistence.*

@Entity
data class Screenshot(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null
) : AuditModel() {
    constructor(id: Long? = null, key: Key) : this(id) {
        this.key = key
    }

    @ManyToOne
    var key: Key? = null

    val filename: String
        get() {
            val nameToHash = "${this.id}_${this.createdAt.toInstant().toEpochMilli()}"
            return "${DigestUtils.sha256Hex(nameToHash.toByteArray())}.png"
        }
}