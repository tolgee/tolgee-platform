package io.tolgee.model.dataImport

import io.tolgee.model.Project
import io.tolgee.model.SoftDeletable
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.validation.constraints.NotNull
import java.util.*

@Entity
class Import(
  @field:NotNull
  @ManyToOne(optional = false)
  val project: Project,
) : StandardAuditModel(), SoftDeletable {
  @field:NotNull
  @ManyToOne(optional = false)
  lateinit var author: UserAccount

  @OneToMany(mappedBy = "import", orphanRemoval = true)
  var files = mutableListOf<ImportFile>()

  override var deletedAt: Date? = null
}
