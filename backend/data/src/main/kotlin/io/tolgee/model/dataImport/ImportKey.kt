package io.tolgee.model.dataImport

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.key.KeyMeta
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToMany
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

@Entity
class ImportKey(
  @field:NotBlank
  @field:Size(max = 2000)
  @Column(length = 2000)
  var name: String,
) : StandardAuditModel(), WithKeyMeta {
  @ManyToMany(mappedBy = "keys")
  @field:NotEmpty
  var files: MutableList<ImportFile> = mutableListOf()

  @OneToMany(mappedBy = "key")
  var translations: MutableList<ImportTranslation> = mutableListOf()

  @OneToOne(mappedBy = "importKey")
  override var keyMeta: KeyMeta? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as ImportKey

    if (name != other.name) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    return result
  }
}
