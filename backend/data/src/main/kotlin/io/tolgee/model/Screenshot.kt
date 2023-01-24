/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.model

import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import org.apache.commons.codec.digest.DigestUtils
import org.hibernate.annotations.ColumnDefault
import javax.persistence.Entity
import javax.persistence.OneToMany

@Entity
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(paths = ["key"])
class Screenshot : StandardAuditModel() {
  @OneToMany(mappedBy = "screenshot", orphanRemoval = true)
  var keyScreenshotReferences: MutableList<KeyScreenshotReference> = mutableListOf()

  /**
   * For legacy projects the path was ${key.project.id}/${key.id}
   */
  var path: String = ""

  val filename: String
    get() {
      return "$path/$hash.$extension"
    }

  val thumbnailFilename: String
    get() {
      if (!hasThumbnail) {
        return filename
      }
      return "$path/${hash}_thumbnail.$extension"
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
