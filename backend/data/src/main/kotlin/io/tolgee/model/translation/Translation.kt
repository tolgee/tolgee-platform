package io.tolgee.model.translation

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Language
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.util.TranslationStatsUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(
      columnNames = ["key_id", "language_id"],
      name = "translation_key_language",
    ),
  ],
  indexes = [
    Index(columnList = "key_id"),
    Index(columnList = "language_id"),
  ],
)
@ActivityLoggedEntity
@EntityListeners(Translation.Companion.UpdateStatsListener::class, Translation.Companion.StateListener::class)
@ActivityEntityDescribingPaths(paths = ["key", "language"])
class Translation(
  @Column(columnDefinition = "text")
  @ActivityLoggedProp
  @ActivityDescribingProp
  var text: String? = null,
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

  fun clear() {
    this.state = TranslationState.UNTRANSLATED
    this.text = null
    this.resetFlags()
  }

  fun isEmpty(): Boolean {
    return this.text.isNullOrEmpty() &&
      !this.outdated &&
      this.mtProvider == null &&
      !this.auto
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as Translation

    if (text != other.text) return false
    if (language.id != other.language.id) return false
    if (key.id != other.key.id) return false
    if (state != other.state) return false
    if (auto != other.auto) return false
    if (mtProvider != other.mtProvider) return false
    return outdated == other.outdated
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (text?.hashCode() ?: 0)
    result = 31 * result + language.id.hashCode()
    result = 31 * result + key.id.hashCode()
    result = 31 * result + state.hashCode()
    result = 31 * result + auto.hashCode()
    result = 31 * result + (mtProvider?.hashCode() ?: 0)
    result = 31 * result + outdated.hashCode()
    return result
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
        if (translation.text == "") {
          translation.text = null
        }

        if (!translation.isEmpty() && translation.state == TranslationState.DISABLED) {
          throw BadRequestException(Message.CANNOT_MODIFY_DISABLED_TRANSLATION)
        }

        if (!translation.text.isNullOrEmpty() && translation.state == TranslationState.UNTRANSLATED) {
          translation.state = TranslationState.TRANSLATED
        }
        if (translation.text.isNullOrEmpty() &&
          translation.state != TranslationState.UNTRANSLATED && translation.state != TranslationState.DISABLED
        ) {
          translation.state = TranslationState.UNTRANSLATED
        }
      }
    }
  }
}
