package io.tolgee.model.views

import io.tolgee.model.Language

interface LanguageView {
  var language: Language
  var base: Boolean
}

class LanguageViewImpl(override var language: Language, override var base: Boolean) : LanguageView {}
