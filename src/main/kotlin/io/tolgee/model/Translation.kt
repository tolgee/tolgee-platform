package io.tolgee.model

import javax.persistence.*

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(
        columnNames = ["key_id", "language_id"],
        name = "translation_key_language"
    )]
)
data class Translation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(columnDefinition = "text")
    var text: String? = null
) : AuditModel() {


    @ManyToOne
    var key: Key? = null

    @ManyToOne
    var language: Language? = null

    constructor(id: Long?, text: String?, key: Key?, language: Language?) : this(id, text) {
        this.key = key
        this.language = language
    }


    class TranslationBuilder internal constructor() {
        private var id: Long? = null
        private var text: String? = null
        private var key: Key? = null
        private var language: Language? = null
        fun id(id: Long?): TranslationBuilder {
            this.id = id
            return this
        }

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
            return Translation(id, text, key, language)
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