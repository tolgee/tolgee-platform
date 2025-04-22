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
  name = "GlossaryTermTranslation",
  indexes = [
    Index(columnList = "textLowercased"),
  ],
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["term_id", "languageTag"]),
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

  @Column(columnDefinition = "text", nullable = false)
  var textLowercased: String? = null // TODO: rename to include something like textFirstWordLowercased

  companion object {
    val WORD_REGEX = Regex("\\p{L}+")

    class GlossaryTermTranslationListener {
      @PrePersist
      @PreUpdate
      fun updateTextLowercased(translation: GlossaryTermTranslation) {
        val locale = Locale.forLanguageTag(translation.languageTag) ?: Locale.ROOT
        translation.textLowercased = translation.text?.lowercase(locale)?.find(WORD_REGEX)
      }
    }
  }
}
