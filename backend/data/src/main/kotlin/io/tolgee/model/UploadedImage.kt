/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.model

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["filename"], name = "uploaded_image_filename"),
  ],
  indexes = [
    Index(columnList = "user_account_id"),
  ],
)
class UploadedImage(
  var filename: String,
  @ManyToOne(fetch = FetchType.LAZY)
  var userAccount: UserAccount,
) : StandardAuditModel() {
  // legacy is jpg, new is png
  @ColumnDefault("jpg")
  var extension: String? = null
    get() = field ?: "jpg"

  val filenameWithExtension
    get() = "$filename.$extension"

  val thumbnailFilenameWithExtension
    get() = "${filename}_thumbnail.$extension"

  var location: String? = null

  var originalWidth: Int = 0

  var originalHeight: Int = 0

  var width: Int = 0

  var height: Int = 0
}
