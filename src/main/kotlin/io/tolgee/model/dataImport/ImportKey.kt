package io.tolgee.model.dataImport

import io.tolgee.model.StandardAuditModel
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

@Entity
class ImportKey(
        @field:NotBlank
        @field:Size(max = 2000)
        @Column(length = 2000)
        var name: String,
) : StandardAuditModel() {
    @ManyToMany(mappedBy = "keys", cascade = [CascadeType.PERSIST])
    @field:NotEmpty
    var files: MutableList<ImportFile> = mutableListOf()

    @OneToMany(mappedBy = "key", cascade = [CascadeType.ALL])
    var translations: MutableList<ImportTranslation> = mutableListOf()

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
