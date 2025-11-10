package io.tolgee.unit

import io.tolgee.dtos.PathDTO
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.LinkedList

class PathDTOTest {
  private val testList =
    LinkedList(
      listOf(*TEST_FULL_PATH.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()),
    )

  private fun getTestList(): LinkedList<String> {
    return LinkedList(testList)
  }

  @Test
  fun testFromFullPath() {
    val pathDTO = PathDTO.fromFullPath("item1.item2.item1.item1.last")
    assertBasicPath(pathDTO)
  }

  @Test
  fun testFromFullPathList() {
    val pathDTO = PathDTO.fromFullPath(testList)
    assertBasicPath(pathDTO)
  }

  @Test
  fun testFromNamePath() {
    val testList = getTestList()
    val name = testList.removeLast()
    val pathDTO = PathDTO.fromPathAndName(java.lang.String.join(".", testList), name)
    Assertions.assertThat(pathDTO.name).isEqualTo(name)
    Assertions.assertThat(pathDTO.fullPath).isEqualTo(getTestList())
  }

  @Test
  fun testFromNamePathList() {
    val testList = getTestList()
    val name = testList.removeLast()
    val pathDTO = PathDTO.fromPathAndName(testList, name)
    Assertions.assertThat(pathDTO.name).isEqualTo(name)
    Assertions.assertThat(pathDTO.fullPath).isEqualTo(getTestList())
  }

  @Test
  fun escaping() {
    val fullPath = "aaa.aaa\\.aaaa.a"
    val testList = PathDTO.fromFullPath(fullPath).fullPath
    Assertions.assertThat(testList).isEqualTo(listOf("aaa", "aaa.aaaa", "a"))
    Assertions.assertThat(PathDTO.fromFullPath(testList).fullPathString).isEqualTo(fullPath)
  }

  private fun assertBasicPath(pathDTO: PathDTO) {
    Assertions.assertThat(pathDTO.fullPath).isEqualTo(testList)
    val testList = getTestList()
    val last = testList.removeLast()
    Assertions.assertThat(pathDTO.path).isEqualTo(testList)
    Assertions.assertThat(pathDTO.name).isEqualTo(last)
  }

  companion object {
    private const val TEST_FULL_PATH = "item1.item2.item1.item1.last"
  }
}
