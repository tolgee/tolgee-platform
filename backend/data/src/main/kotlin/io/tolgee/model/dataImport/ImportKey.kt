package io.tolgee.model.dataImport

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.key.KeyMeta
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.Type

@Entity
class ImportKey(
  @field:NotBlank
  @field:Size(max = 2000)
  @Column(length = 2000)
  var name: String,
  @ManyToOne
  var file: ImportFile,
) : StandardAuditModel(), WithKeyMeta {
  @OneToMany(mappedBy = "key", orphanRemoval = true)
  var translations: MutableList<ImportTranslation> = mutableListOf()

  @OneToOne(mappedBy = "importKey")
  override var keyMeta: KeyMeta? = null

  var pluralArgName: String? = null

  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var nestedKeyPath: List<Any>? = null

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
