package io.tolgee.formats.ios.`in`

fun guessNamespaceFromPath(filePath: String): String {
  return REGEX.find(filePath)?.groups?.get("namespace")?.value ?: ""
}

val REGEX = "(?<namespace>[\\w-.&#$@{}*^~\\s]+)/[\\w-]+\\.lproj/.*".toRegex()
