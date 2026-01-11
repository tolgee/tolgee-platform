package io.tolgee.model.dataImport

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.key.KeyMeta
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(
  indexes = [
    Index(columnList = "file_id"),
  ],
)
class ImportKey(
  @field:NotBlank
  @field:Size(max = 2000)
  @Column(length = 2000)
  var name: String,
  @ManyToOne
  var file: ImportFile,
) : StandardAuditModel(),
  WithKeyMeta {
  @OneToMany(mappedBy = "key", orphanRemoval = true)
  var translations: MutableList<ImportTranslation> = mutableListOf()

  @OneToOne(mappedBy = "importKey")
  override var keyMeta: KeyMeta? = null

  var pluralArgName: String? = null

  @ColumnDefault("true")
  var shouldBeImported: Boolean = true

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as ImportKey

    return name == other.name
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    return result
  }
}
