/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.model

import io.tolgee.model.key.Key
import org.apache.commons.codec.digest.DigestUtils
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity

data class Screenshot(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0
) : AuditModel() {
  constructor(id: Long = 0, key: Key) : this(id) {
    this.key = key
  }

  @Suppress("JoinDeclarationAndAssignment")
  @ManyToOne(optional = false)
  lateinit var key: Key

  val filename: String
    get() {
      val nameToHash = "${this.id}_${this.createdAt!!.toInstant().toEpochMilli()}"
      val fileName = DigestUtils.sha256Hex(nameToHash.toByteArray())
      return "${key.project!!.id}/${key.id}/$fileName.jpg"
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Screenshot

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}
