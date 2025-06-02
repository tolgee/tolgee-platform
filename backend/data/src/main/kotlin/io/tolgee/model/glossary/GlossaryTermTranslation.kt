package io.tolgee.model.glossary

import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import io.tolgee.util.find
import jakarta.persistence.*
import java.util.*

@Entity
@EntityListeners(GlossaryTermTranslation.Companion.GlossaryTermTranslationListener::class)
@ActivityLoggedEntity
@Table(
  indexes = [
    Index(columnList = "first_word_lowercased"),
    Index(columnList = "term_id"),
  ],
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["term_id", "language_tag"]),
  ],
)
class GlossaryTermTranslation(
  var languageTag: String,
  @Column(columnDefinition = "text", nullable = false)
  @ActivityLoggedProp
  var text: String? = null,
) : StandardAuditModel() {
  @ManyToOne
  lateinit var term: GlossaryTerm

  @Column(name = "first_word_lowercased", columnDefinition = "varchar(127)", nullable = false)
  var firstWordLowercased: String? = null

  companion object {
    val WORD_REGEX = Regex("\\p{L}+")

    class GlossaryTermTranslationListener {
      @PrePersist
      @PreUpdate
      fun updateFirstWordLowercased(translation: GlossaryTermTranslation) {
        val locale = Locale.forLanguageTag(translation.languageTag) ?: Locale.ROOT
        var firstWordLowercased = translation.text?.lowercase(locale)?.find(WORD_REGEX)
        if (firstWordLowercased != null && firstWordLowercased.length > 127) {
          firstWordLowercased = firstWordLowercased.substring(0, 127)
        }
        translation.firstWordLowercased = firstWordLowercased
      }
    }
  }
}
