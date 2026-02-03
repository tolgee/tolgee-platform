package io.tolgee.util

val IGNORED_REGEXES by lazy {
  arrayOf(
    // contents.json from Apple's xcloc
    "^[\\w-_]+\\.xcloc/contents.json$".toRegex(),
    "^.+\\.xcloc/Source Contents/Localizable.xcstrings$".toRegex(),
    "^.+\\.xcloc/Source Contents/.*InfoPlist.xcstrings$".toRegex(),
    // .DS_Store files
    """(.*/|^)\.DS_Store$""".toRegex(),
  )
}

inline fun <reified T> filterFiles(files: List<Pair<String, T>>): Collection<T> {
  return files
    .filter { file -> !IGNORED_REGEXES.any { it.matches(file.first) } }
    .map { it.second }
    .toList()
}
