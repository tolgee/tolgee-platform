/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.model

import org.hibernate.annotations.ColumnDefault
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["filename"], name = "uploaded_image_filename")])
class UploadedImage(
  var filename: String,

  @ManyToOne(fetch = FetchType.LAZY)
  var userAccount: UserAccount
) : StandardAuditModel() {

  @ColumnDefault("jpg")
  var extension: String? = null
    get() = field ?: "jpg"

  val filenameWithExtension
    get() = "$filename.$extension"
}
