package io.tolgee.model.translation

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationCommentState
import org.hibernate.validator.constraints.Length
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.validation.constraints.NotBlank

@Entity
data class TranslationComment(
        @field:Length(max = 10000)
        @field:NotBlank()
        @Column(columnDefinition = "text")
        var text: String = "",

        @ManyToOne
        var author: UserAccount,

        var state: TranslationCommentState = TranslationCommentState.RESOLUTION_NOT_NEEDED
) : StandardAuditModel() {

}
