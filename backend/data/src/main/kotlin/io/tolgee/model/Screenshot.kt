/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.model

import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.model.key.Key
import org.apache.commons.codec.digest.DigestUtils
import org.hibernate.annotations.ColumnDefault
import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(paths = ["key"])
class Screenshot : StandardAuditModel() {
  @ManyToOne(optional = false)
  lateinit var key: Key

  val filename: String
    get() {
      return "${key.project.id}/${key.id}/$hash.$extension"
    }

  val thumbnailFilename: String
    get() {
      if (!hasThumbnail) {
        return filename
      }
      return "${key.project.id}/${key.id}/${hash}_thumbnail.$extension"
    }

  val hash: String
    get() {
      val nameToHash = "${this.id}_${this.createdAt!!.toInstant().toEpochMilli()}"
      return DigestUtils.sha256Hex(nameToHash.toByteArray())
    }

  @ColumnDefault("jpg")
  var extension: String? = null
    get() = field ?: "jpg"

  @ColumnDefault("false")
  var hasThumbnail: Boolean = true

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
