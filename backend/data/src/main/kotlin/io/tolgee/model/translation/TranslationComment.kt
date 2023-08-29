package io.tolgee.model.translation

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationCommentState
import org.hibernate.validator.constraints.Length
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType.LAZY
import javax.persistence.Index
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotBlank

@Entity
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(paths = ["translation"])
@Table(
  indexes = [Index(columnList = "state")]
)
class TranslationComment(
  @field:Length(max = 10000)
  @field:NotBlank
  @Column(columnDefinition = "text")
  @ActivityLoggedProp
  @ActivityDescribingProp
  var text: String = "",

  @ActivityLoggedProp
  var state: TranslationCommentState = TranslationCommentState.NEEDS_RESOLUTION,

  @ManyToOne
  var translation: Translation
) : StandardAuditModel() {
  @ManyToOne(optional = false, fetch = LAZY)
  lateinit var author: UserAccount
}
