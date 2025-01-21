/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.model

import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.apache.commons.codec.digest.DigestUtils
import org.hibernate.annotations.ColumnDefault

@Entity
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(paths = ["key"])
@Table(indexes = [Index(name = "screenshot_location_idx", columnList = "location")])
class Screenshot : StandardAuditModel() {
  @OneToMany(mappedBy = "screenshot", orphanRemoval = true)
  var keyScreenshotReferences: MutableList<KeyScreenshotReference> = mutableListOf()

  /**
   * For legacy projects the path was ${key.project.id}/${key.id}
   */
  var path: String = ""

  val pathWithSlash: String
    get() = if (path.isEmpty()) "" else "$path/"

  val filename: String
    get() {
      return "$pathWithSlash$hash.$extension"
    }

  val middleSizedFilename: String?
    get() {
      if (!hasMiddleSized) {
        return null
      }
      return "$pathWithSlash${hash}_middle_sized.$extension"
    }

  val thumbnailFilename: String
    get() {
      if (!hasThumbnail) {
        return filename
      }
      return "$pathWithSlash${hash}_thumbnail.$extension"
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

  @ColumnDefault("false")
  var hasMiddleSized: Boolean = true

  var location: String? = null

  @ColumnDefault("0")
  var width: Int = 0

  @ColumnDefault("0")
  var height: Int = 0

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Screenshot

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}
