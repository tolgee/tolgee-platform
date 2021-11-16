package io.tolgee.model.translation

import io.tolgee.model.Language
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import org.hibernate.annotations.ColumnDefault
import org.hibernate.envers.Audited
import javax.persistence.*
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
@Audited
class Translation(
  @Column(columnDefinition = "text")
  var text: String? = null
) : StandardAuditModel() {
  @ManyToOne(optional = false)
  @field:NotNull
  var key: Key? = null

  @ManyToOne
  var language: Language? = null

  @Enumerated
  @ColumnDefault(value = "2")
  var state: TranslationState = TranslationState.TRANSLATED

  @OneToMany(mappedBy = "translation", cascade = [CascadeType.REMOVE])
  var comments: MutableList<TranslationComment> = mutableListOf()

  constructor(text: String?, key: Key?, language: Language?) : this(text) {
    this.key = key
    this.language = language
  }

  class TranslationBuilder internal constructor() {
    private var id: Long? = null
    private var text: String? = null
    private var key: Key? = null
    private var language: Language? = null

    fun text(text: String?): TranslationBuilder {
      this.text = text
      return this
    }

    fun key(key: Key?): TranslationBuilder {
      this.key = key
      return this
    }

    fun language(language: Language?): TranslationBuilder {
      this.language = language
      return this
    }

    fun build(): Translation {
      return Translation(text, key, language)
    }

    override fun toString(): String {
      return "Translation.TranslationBuilder(id=$id, text=$text, key=$key, language=$language)"
    }
  }

  companion object {
    @JvmStatic
    fun builder(): TranslationBuilder {
      return TranslationBuilder()
    }
  }
}
