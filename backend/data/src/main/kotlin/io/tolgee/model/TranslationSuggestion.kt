package io.tolgee.model

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.key.Key
import jakarta.persistence.*

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "key_id"),
    Index(columnList = "language_id"),
    Index(columnList = "author_id"),
  ],
)
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(["key", "language"])
class TranslationSuggestion(
  @ManyToOne
  var project: Project,
  @ManyToOne
  var key: Key? = null,
  @ManyToOne
  var language: Language? = null,
  @ManyToOne
  var author: UserAccount? = null,
  @Column(columnDefinition = "text")
  @ActivityLoggedProp
  @ActivityDescribingProp
  var translation: String? = null,
  @Enumerated(EnumType.STRING)
  @ActivityLoggedProp
  var state: TranslationSuggestionState = TranslationSuggestionState.ACTIVE,
) : StandardAuditModel()
