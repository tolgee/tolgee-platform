/*
 * Copyright (c) 2020. Polygloat
 */

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

    @ManyToOne(optional = false)
    lateinit var key: Key

    val filename: String
        get() {
            val nameToHash = "${this.id}_${this.createdAt.toInstant().toEpochMilli()}"
            val fileName = DigestUtils.sha256Hex(nameToHash.toByteArray())
            return "${key.repository!!.id}/${key.id}/${fileName}.jpg"
        }
}