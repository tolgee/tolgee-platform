package io.tolgee.service.queryBuilders

interface Cursorable {
  fun toCursorValue(property: String): String?
}
