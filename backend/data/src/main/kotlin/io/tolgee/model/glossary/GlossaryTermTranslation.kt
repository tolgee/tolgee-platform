package io.tolgee.model.glossary

import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import io.tolgee.util.find
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.Locale

@Entity
@EntityListeners(GlossaryTermTranslation.Companion.GlossaryTermTranslationListener::class)
@ActivityLoggedEntity
@Table(
  indexes = [
    Index(columnList = "language_tag"),
    Index(columnList = "first_word_lowercased"),
    Index(columnList = "term_id"),
  ],
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["term_id", "language_tag"]),
  ],
)
@ActivityEntityDescribingPaths(paths = ["term.glossary", "term"])
class GlossaryTermTranslation(
  var languageTag: String,
  @Column(columnDefinition = "text", nullable = false)
  @ActivityLoggedProp
  var text: String = "",
) : StandardAuditModel() {
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  lateinit var term: GlossaryTerm

  @Column(name = "first_word_lowercased", columnDefinition = "varchar(127)", nullable = true)
  var firstWordLowercased: String? = null

  companion object {
    val WORD_REGEX = Regex("\\p{L}+")

    class GlossaryTermTranslationListener {
      @PrePersist
      @PreUpdate
      fun updateFirstWordLowercased(translation: GlossaryTermTranslation) {
        val locale = Locale.forLanguageTag(translation.languageTag) ?: Locale.ROOT
        translation.firstWordLowercased =
          translation.text
            .lowercase(locale)
            .find(WORD_REGEX)
            ?.take(127)
      }
    }
  }
}
