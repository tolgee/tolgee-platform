package io.tolgee.model.translation

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationCommentState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

@Entity
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(paths = ["translation"])
@Table(
  indexes = [
    Index(columnList = "state"),
    Index(columnList = "translation_id"),
    Index(columnList = "author_id"),
  ],
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
  var translation: Translation,
) : StandardAuditModel() {
  @ManyToOne(optional = false, fetch = LAZY)
  lateinit var author: UserAccount
}
