package io.tolgee.util

val IGNORED_REGEXES by lazy {
  arrayOf(
    // contents.json from Apple's xcloc
    "^[\\w-_]+\\.xcloc/contents.json$".toRegex(),
  )
}

inline fun <reified T> filterFiles(files: Map<String, T>): Collection<T> {
  return files.filter { file -> !IGNORED_REGEXES.any { it.matches(file.key) } }.values.toList()
}
