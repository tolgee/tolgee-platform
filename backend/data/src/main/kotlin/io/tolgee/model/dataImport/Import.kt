package io.tolgee.model.dataImport

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["author_id", "project_id"])])
class Import(
  @field:NotNull
  @ManyToOne(optional = false)
  val author: UserAccount,

  @field:NotNull
  @ManyToOne(optional = false)
  val project: Project
) : StandardAuditModel() {

  @OneToMany(mappedBy = "import")
  var files = mutableListOf<ImportFile>()
}
