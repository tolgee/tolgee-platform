package io.tolgee.formats.apple.`in`

fun guessNamespaceFromPath(filePath: String): String {
  val guessed =
    REGEX
      .find(filePath)
      ?.groups
      ?.get("namespace")
      ?.value ?: return ""
  // "Localizable" is default tableName in Apple suite
  if (guessed == "Localizable") {
    return ""
  }
  return guessed
}

val REGEX = "(?:[\\w-]+\\.lproj/)?(?<namespace>[\\w-.&#\$@{}*^~\\s]+)\\.strings(?:dict)?".toRegex()
