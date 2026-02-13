package io.tolgee.model.dataImport

import io.tolgee.model.Project
import io.tolgee.model.SoftDeletable
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.util.Date

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "author_id"),
    Index(columnList = "branch_id"),
  ],
)
class Import(
  @field:NotNull
  @ManyToOne(optional = false)
  val project: Project,
) : StandardAuditModel(),
  SoftDeletable {
  @field:NotNull
  @ManyToOne(optional = false)
  lateinit var author: UserAccount

  @OneToMany(mappedBy = "import", orphanRemoval = true)
  var files = mutableListOf<ImportFile>()

  @ManyToOne(targetEntity = Branch::class, optional = false)
  @JoinColumn(name = "branch_id", nullable = false)
  lateinit var branch: Branch

  override var deletedAt: Date? = null
}
