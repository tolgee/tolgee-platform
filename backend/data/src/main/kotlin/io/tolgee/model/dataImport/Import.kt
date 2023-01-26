package io.tolgee.model.dataImport

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotNull

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["author_id", "project_id"])])
class Import(
  @field:NotNull
  @ManyToOne(optional = false)
  val project: Project
) : StandardAuditModel() {

  @field:NotNull
  @ManyToOne(optional = false)
  lateinit var author: UserAccount

  @OneToMany(mappedBy = "import", orphanRemoval = true)
  var files = mutableListOf<ImportFile>()
}
