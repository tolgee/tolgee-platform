package io.tolgee.dtos

import io.tolgee.helpers.TextHelper.splitOnNonEscapedDelimiter
import java.util.LinkedList
import java.util.stream.Collectors

class PathDTO private constructor() {
  var fullPath: MutableList<String> = mutableListOf()

  val name: String
    get() = fullPath.last()
  val fullPathString: String
    get() =
      fullPath
        .stream()
        .map { i: String ->
          i.replace(
            ("\\" + DELIMITER).toRegex(),
            "\\\\" + DELIMITER,
          )
        }.collect(
          Collectors.joining("."),
        )
  val path: List<String>
    get() {
      val path = LinkedList(fullPath)
      path.removeLast()
      return path
    }

  private fun add(item: String) {
    fullPath.add(item)
  }

  private fun add(list: List<String>) {
    fullPath.addAll(list)
  }

  fun setFullPath(fullPath: LinkedList<String>) {
    this.fullPath = fullPath
  }

  override fun toString(): String {
    return "PathDTO(fullPath=" + fullPath + ")"
  }

  companion object {
    const val DELIMITER = '.'

    fun fromFullPath(fullPath: String?): PathDTO {
      val pathDTO = PathDTO()
      pathDTO.add(splitOnNonEscapedDelimiter(fullPath!!, DELIMITER))
      return pathDTO
    }

    fun fromFullPath(path: List<String>): PathDTO {
      val pathDTO = PathDTO()
      pathDTO.add(path)
      return pathDTO
    }

    fun fromPathAndName(
      path: String,
      name: String,
    ): PathDTO {
      val pathDTO = PathDTO()
      var items = splitOnNonEscapedDelimiter(path, DELIMITER)
      if (path.isEmpty()) {
        items = emptyList()
      }
      pathDTO.add(items)
      pathDTO.add(name)
      return pathDTO
    }

    fun fromPathAndName(
      path: List<String>,
      name: String,
    ): PathDTO {
      val pathDTO = PathDTO()
      pathDTO.add(path)
      pathDTO.add(name)
      return pathDTO
    }
  }
}
