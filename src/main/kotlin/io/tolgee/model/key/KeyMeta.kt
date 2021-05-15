package io.tolgee.model.key

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.ImportKey
import javax.persistence.*

@Entity
@EntityListeners(KeyMeta.Companion.KeyMetaListener::class)
class KeyMeta(
        @OneToOne
        var key: Key? = null,

        @OneToOne
        var importKey: ImportKey? = null
) : StandardAuditModel() {
    companion object {
        class KeyMetaListener {
            @PrePersist
            @PreUpdate
            fun preSave(keyMeta: KeyMeta) {
                if (!(keyMeta.key == null).xor(keyMeta.importKey == null)) {
                    throw Exception("Exactly one of key or importKey must be set!")
                }
            }
        }
    }
}
