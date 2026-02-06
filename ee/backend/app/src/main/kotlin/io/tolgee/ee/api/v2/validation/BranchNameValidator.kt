package io.tolgee.ee.api.v2.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class BranchNameValidator : ConstraintValidator<ValidBranchName, String> {
  override fun isValid(
    value: String?,
    context: ConstraintValidatorContext?,
  ): Boolean {
    if (value == null) return false

    // Allowed characters only: a-z, 0-9, . - _ / (lowercase)
    val allowedPattern = Regex("^[a-z0-9.\\-_/]+$")
    if (!allowedPattern.matches(value)) return false

    // Start: letter, number, or dot
    val startPattern = Regex("^[a-z0-9.]")
    if (!startPattern.containsMatchIn(value)) return false

    // End: letter, number, or dot
    val endPattern = Regex("[a-z0-9.]$")
    if (!endPattern.containsMatchIn(value)) return false

    // No consecutive slashes
    if (value.contains("//")) return false

    // No consecutive dots
    if (value.contains("..")) return false

    // Check slash-separated parts
    val parts = value.split("/")
    for (i in 1 until parts.size) {
      // Parts after first slash cannot start with dot
      if (parts[i].startsWith(".")) return false
    }

    // No part can end with .lock
    for (part in parts) {
      if (part.endsWith(".lock")) return false
    }

    return true
  }
}
