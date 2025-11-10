package io.tolgee.model.dataImport

import io.tolgee.api.IImportSettings
import io.tolgee.model.AuditModel
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault

@Entity
@IdClass(ImportSettingsId::class)
@Table(
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "user_account_id"),
  ],
)
class ImportSettings(
  @Id
  @ManyToOne
  val project: Project,
) : AuditModel(),
  IImportSettings {
  @ManyToOne
  @Id
  lateinit var userAccount: UserAccount

  @ColumnDefault("false")
  override var overrideKeyDescriptions: Boolean = false

  @ColumnDefault("true")
  override var convertPlaceholdersToIcu: Boolean = true

  @ColumnDefault("true")
  override var createNewKeys: Boolean = true
}
