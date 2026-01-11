package io.tolgee.component.machineTranslation

object LanguageTagConvertor {
  private const val MAX_ITERATIONS = 20

  fun findSuitableTag(
    suitableTags: Array<String>,
    desiredTag: String,
  ): String? {
    // in Tolgee platform Traditional Chinese is zh-Hant, but AWS translate has is as zh-TW
    if (desiredTag === "zh-Hant" && suitableTags.contains("zh-TW")) {
      return "zh-TW"
    }

    return findSuitableTag(desiredTag) { newTag ->
      suitableTags.contains(newTag)
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
