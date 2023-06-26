package io.tolgee.model.translation

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.constants.MtServiceType
import io.tolgee.model.Language
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.util.TranslationStatsUtil
import org.hibernate.annotations.ColumnDefault
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.PrePersist
import javax.persistence.PreUpdate
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotNull

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(
      columnNames = ["key_id", "language_id"],
      name = "translation_key_language"
    )
  ]
)
@ActivityLoggedEntity
@EntityListeners(Translation.Companion.UpdateStatsListener::class, Translation.Companion.StateListener::class)
@ActivityEntityDescribingPaths(paths = ["key", "language"])
class Translation(
  @Column(columnDefinition = "text")
  @ActivityLoggedProp
  @ActivityDescribingProp
  var text: String? = null
) : StandardAuditModel() {
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @field:NotNull
  lateinit var key: Key

  @ManyToOne
  lateinit var language: Language

  @Enumerated
  @ColumnDefault(value = "2")
  @ActivityLoggedProp
  var state: TranslationState = TranslationState.TRANSLATED

  /**
   * Was translated automatically?
   */
  @ColumnDefault("false")
  @ActivityLoggedProp
  var auto: Boolean = false

  /**
   * Which machine translation provider was used to translate this value?
   */
  @Enumerated
  @ActivityLoggedProp
  var mtProvider: MtServiceType? = null

  @OneToMany(mappedBy = "translation", orphanRemoval = true)
  var comments: MutableList<TranslationComment> = mutableListOf()

  var wordCount: Int? = null

  var characterCount: Int? = null

  @ActivityLoggedProp
  @field:ColumnDefault("false")
  var outdated: Boolean = false

  constructor(text: String? = null, key: Key, language: Language) : this(text) {
    this.key = key
    this.language = language
  }

  fun resetFlags() {
    this.outdated = false
    this.mtProvider = null
    this.auto = false
  }

  companion object {
    class UpdateStatsListener {
      @PrePersist
      @PreUpdate
      fun preSave(translation: Translation) {
        translation.characterCount = TranslationStatsUtil.getCharacterCount(translation.text)
        translation.wordCount = TranslationStatsUtil.getWordCount(translation.text, translation.language.tag)
      }
    }

    class StateListener {
      @PrePersist
      @PreUpdate
      fun preSave(translation: Translation) {
        if (!translation.text.isNullOrEmpty() && translation.state == TranslationState.UNTRANSLATED) {
          translation.state = TranslationState.TRANSLATED
        }
        if (translation.text.isNullOrEmpty() && translation.state != TranslationState.UNTRANSLATED) {
          translation.state = TranslationState.UNTRANSLATED
        }
      }
    }
  }
}
