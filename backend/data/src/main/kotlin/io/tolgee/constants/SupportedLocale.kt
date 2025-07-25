package io.tolgee.constants

enum class SupportedLocale(val code: String) {
  EN("en"),
  CS("cs"),
  FR("fr"),
  ES("es"),
  DE("de"),
  PT("pt"),
  DA("da"),
  JA("ja");

  companion object {
    val DEFAULT = EN

    fun fromCode(code: String): SupportedLocale? {
      return entries.find { it.code.equals(code, ignoreCase = true) }
    }
  }
}
