package io.tolgee.util

object EmailNormalizer {
  // Keep in sync with the SQL expression used by
  // UserAccountRepository.existsActiveByNormalizedEmail and its backing functional index
  // (regexp_replace(lower(username), '\+[^@]*@', '@')). The index is only used when the two match.
  private val ALIAS_REGEX = Regex("\\+[^@]*@")

  fun normalize(email: String): String {
    val lowered = email.lowercase()
    return ALIAS_REGEX.replaceFirst(lowered, "@")
  }

  fun domainOf(email: String): String? {
    if (email.count { it == '@' } != 1) {
      return null
    }
    return email.substringAfter('@').lowercase()
  }
}
