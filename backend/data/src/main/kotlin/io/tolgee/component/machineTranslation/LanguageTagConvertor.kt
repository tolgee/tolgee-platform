package io.tolgee.component.machineTranslation

object LanguageTagConvertor {
  private const val MAX_ITERATIONS = 20

  fun findSuitableTag(
    suitableTags: Array<String>,
    desiredTag: String,
  ): String? {
    return findSuitableTag(desiredTag) { newTag ->
      suitableTags.any { it.equals(newTag, ignoreCase = true) }
    }
  }

  fun findSuitableTag(
    desiredTag: String,
    validateFn: (newTag: String) -> Boolean,
  ): String? {
    if (validateFn(desiredTag)) {
      return desiredTag
    }

    var newTag = desiredTag
    var iterations = 0

    while (newTag != "") {
      newTag = newTag.replace(Regex("[-_]?[a-zA-Z0-9]+$"), "")
      if (validateFn(newTag)) {
        return newTag
      }

      if (iterations++ >= MAX_ITERATIONS) {
        break
      }
    }
    return null
  }
}
