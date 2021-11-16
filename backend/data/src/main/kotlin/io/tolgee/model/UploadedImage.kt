/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.model

import javax.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["filename"], name = "uploaded_image_filename")])
class UploadedImage(
  var filename: String,

  @ManyToOne(fetch = FetchType.LAZY)
  var userAccount: UserAccount
) : StandardAuditModel() {

  val filenameWithExtension
    get() = "$filename.jpg"
}
