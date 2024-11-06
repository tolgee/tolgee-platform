package io.tolgee.model.dataImport

import io.tolgee.model.Language
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.Size

@Entity
@Table(
  indexes = [
    Index(columnList = "file_id"),
    Index(columnList = "existing_language_id"),
  ],
)
class ImportLanguage(
  @Size(max = 2000)
  @Column(length = 2000)
  var name: String,
  @ManyToOne(optional = false)
  var file: ImportFile,
) : StandardAuditModel() {
  @OneToMany(mappedBy = "language", orphanRemoval = true)
  var translations: MutableList<ImportTranslation> = mutableListOf()

  @ManyToOne
  var existingLanguage: Language? = null
}
