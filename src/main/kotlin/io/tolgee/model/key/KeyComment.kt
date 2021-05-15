package io.tolgee.model.key

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne

@Entity
class KeyComment(
        @ManyToOne(optional = false)
        var keyMeta: KeyMeta,

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        var author: UserAccount,

        @Column(columnDefinition = "text", length = 2000)
        var text: String,

        var fromImport: Boolean = false
) : StandardAuditModel() {
    override fun toString(): String {
        return "KeyComment(text='$text', fromImport=$fromImport)"
    }
}
