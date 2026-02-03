package io.tolgee.model

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.key.Key
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id,language_id,key_id"),
    Index(columnList = "author_id"),
  ],
)
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(["key", "language"])
class TranslationSuggestion(
  @ManyToOne
  @JoinColumn(nullable = false)
  var project: Project,
  @ManyToOne
  @JoinColumn(nullable = false)
  var language: Language? = null,
  @ManyToOne
  @JoinColumn(nullable = false)
  var author: UserAccount? = null,
  @Column(columnDefinition = "text", nullable = false)
  @ActivityLoggedProp
  @ActivityDescribingProp
  var translation: String? = null,
  @ActivityLoggedProp
  @ColumnDefault("false")
  var isPlural: Boolean = false,
  @Enumerated(EnumType.STRING)
  @ActivityLoggedProp
  var state: TranslationSuggestionState = TranslationSuggestionState.ACTIVE,
) : StandardAuditModel() {
  @ManyToOne
  @JoinColumn(nullable = false)
  lateinit var key: Key
}
