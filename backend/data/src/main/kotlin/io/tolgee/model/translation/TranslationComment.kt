package io.tolgee.model.translation

import io.tolgee.activity.ActivityLogged
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationCommentState
import org.hibernate.envers.Audited
import org.hibernate.validator.constraints.Length
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.validation.constraints.NotBlank

@Entity
@Audited
@ActivityLogged
class TranslationComment(
  @field:Length(max = 10000)
  @field:NotBlank
  @Column(columnDefinition = "text")
  @ActivityLogged
  var text: String = "",

  @ActivityLogged
  var state: TranslationCommentState = TranslationCommentState.NEEDS_RESOLUTION,

  @ManyToOne
  var translation: Translation
) : StandardAuditModel() {
  @ManyToOne(optional = false)
  lateinit var author: UserAccount
}
