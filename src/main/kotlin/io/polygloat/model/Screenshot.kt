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
            val keyFolder = DigestUtils.sha256Hex(key.id.toString())
            val repositoryFolder = DigestUtils.sha256Hex(key.repository!!.id.toString())
            return "${repositoryFolder}/${keyFolder}/${fileName}.jpg"
        }
}