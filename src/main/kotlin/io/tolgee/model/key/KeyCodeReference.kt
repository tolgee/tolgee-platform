package io.tolgee.model.key

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne

@Entity
class KeyCodeReference(
        @ManyToOne(optional = false)
        var keyMeta: KeyMeta,

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        var author: UserAccount,

        @Column(length = 300)
        var path: String,

        var line: Long,

        var fromImport: Boolean = false
) : StandardAuditModel() {
    override fun toString(): String {
        return "KeyCodeReference(path='$path', line=$line)"
    }
}
