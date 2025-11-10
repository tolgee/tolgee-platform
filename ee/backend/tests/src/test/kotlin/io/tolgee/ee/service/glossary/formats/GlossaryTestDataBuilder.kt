package io.tolgee.ee.service.glossary.formats

import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation
import io.tolgee.model.glossary.Glossary_.terms

fun createGlossary(
  baseLanguageTag: String,
  name: String = "Test Glossary",
): Glossary {
  return Glossary().apply {
    this.name = name
    this.baseLanguageTag = baseLanguageTag
  }
}

fun createTerm(description: String? = null): GlossaryTerm {
  return GlossaryTerm().apply {
    this.description = description ?: ""
    this.translations = mutableListOf()
  }
}

fun GlossaryTerm.addTranslation(
  languageTag: String,
  text: String,
): GlossaryTerm {
  translations.add(
    GlossaryTermTranslation(languageTag, text).apply {
      term = this@addTranslation
    },
  )
  return this
}

fun GlossaryTerm.withFlags(
  nonTranslatable: Boolean? = null,
  caseSensitive: Boolean? = null,
  abbreviation: Boolean? = null,
  forbiddenTerm: Boolean? = null,
): GlossaryTerm {
  nonTranslatable?.let { flagNonTranslatable = it }
  caseSensitive?.let { flagCaseSensitive = it }
  abbreviation?.let { flagAbbreviation = it }
  forbiddenTerm?.let { flagForbiddenTerm = it }
  return this
}

class GlossaryBuilder {
  private var name: String = "Test Glossary"
  private lateinit var baseLanguageTag: String
  private val terms = mutableListOf<GlossaryTerm>()

  fun withName(name: String) =
    apply {
      this.name = name
    }

  fun withBaseLanguageTag(baseLanguageTag: String) =
    apply {
      this.baseLanguageTag = baseLanguageTag
    }

  fun withTerm(term: GlossaryTerm) =
    apply {
      terms.add(term)
    }

  fun build(): Glossary {
    return createGlossary(baseLanguageTag, name).also {
      it.terms = terms.toMutableList()
      terms.forEach { term -> term.glossary = it }
    }
  }
}

fun glossary(block: GlossaryBuilder.() -> Unit): Glossary {
  return GlossaryBuilder().apply(block).build()
}

class GlossaryTermBuilder {
  private var description: String? = null
  private val translations = mutableListOf<Pair<String, String>>()
  private var flagNonTranslatable: Boolean = false
  private var flagCaseSensitive: Boolean = false
  private var flagAbbreviation: Boolean = false
  private var flagForbiddenTerm: Boolean = false

  fun withDescription(description: String) =
    apply {
      this.description = description
    }

  fun withTranslation(
    languageTag: String,
    text: String,
  ) = apply {
    translations.add(languageTag to text)
  }

  fun withTranslations(vararg translations: Pair<String, String>) =
    apply {
      this.translations.addAll(translations)
    }

  fun translatable(value: Boolean = true) =
    apply {
      flagNonTranslatable = !value
    }

  fun caseSensitive(value: Boolean = true) =
    apply {
      flagCaseSensitive = value
    }

  fun abbreviation(value: Boolean = true) =
    apply {
      flagAbbreviation = value
    }

  fun forbiddenTerm(value: Boolean = true) =
    apply {
      flagForbiddenTerm = value
    }

  fun build(): GlossaryTerm {
    return createTerm(description).let {
      translations.forEach { (languageTag, text) ->
        it.addTranslation(languageTag, text)
      }
      it.withFlags(
        nonTranslatable = flagNonTranslatable,
        caseSensitive = flagCaseSensitive,
        abbreviation = flagAbbreviation,
        forbiddenTerm = flagForbiddenTerm,
      )
    }
  }
}

fun GlossaryBuilder.glossaryTerm(block: GlossaryTermBuilder.() -> Unit): GlossaryTerm {
  return GlossaryTermBuilder().apply(block).build().also {
    withTerm(it)
  }
}
