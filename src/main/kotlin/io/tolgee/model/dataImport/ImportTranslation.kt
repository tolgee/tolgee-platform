package io.tolgee.model.dataImport

import com.sun.istack.NotNull
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.Translation
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

@Entity
class ImportTranslation(
        @Column(columnDefinition = "text", length = 2000)
        var text: String?,

        @ManyToOne
        var language: ImportLanguage,
) : StandardAuditModel() {
    @ManyToOne(optional = false)
    lateinit var key: ImportKey

    @OneToOne
    var conflict: Translation? = null

    /**
     * Whether this translation will override the conflict
     */
    @field:NotNull
    var override: Boolean = false

    /**
     * Whether user explicitely resolved this conflict
     */
    @field:NotNull
    var resolved: Boolean = false
}
