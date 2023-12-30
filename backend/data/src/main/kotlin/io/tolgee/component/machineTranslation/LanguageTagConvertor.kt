package io.tolgee.component.machineTranslation

object LanguageTagConvertor {
  fun findSuitableTag(
    suitableTags: Array<String>,
    desiredLanguage: String,
  ): String? {
    if (suitableTags.contains(desiredLanguage)) {
      return desiredLanguage
    }

    // in Tolgee platform Traditional Chinese is zh-Hant, but AWS translate has is as zh-TW
    if (desiredLanguage === "zh-Hant" && suitableTags.contains("zh-TW")) {
      return "zh-TW"
    }

    var desired = desiredLanguage

    while (desired != "") {
      desired = desired.replace(Regex("[-_]?[a-zA-Z0-9]+$"), "")
      if (suitableTags.contains(desired)) {
        return desired
      }
    }

    return null
  }
}
