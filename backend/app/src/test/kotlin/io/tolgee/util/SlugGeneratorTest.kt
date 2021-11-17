package io.tolgee.util

import io.tolgee.exceptions.BadRequestException
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.assertions.Assertions.assertThatThrownBy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.annotations.Test

@SpringBootTest
class SlugGeneratorTest : AbstractTestNGSpringContextTests() {

  @field:Autowired
  lateinit var slugGenerator: SlugGenerator

  @Test
  fun testGenerateEmpty() {
    assertThat(slugGenerator.generate("", 3, 10) { true }).isEqualTo("a10")
  }

  @Test
  fun testValidInput() {
    assertThat(slugGenerator.generate("Hello I am cool Project name", 3, 50) { true })
      .isEqualTo("hello-i-am-cool-project-name")
  }

  @Test
  fun throwsWhenTooHard() {
    assertThatThrownBy {
      slugGenerator.generate("Hello I am cool Project name", 3, 50) { false }
    }.isInstanceOf(BadRequestException::class.java)
  }

  @Test
  fun removesAccents() {
    assertThat(slugGenerator.generate("Toto je český žlutý koníček", 3, 50) { true })
      .isEqualTo("toto-je-cesky-zluty-konicek")
  }

  @Test
  fun shortenString() {
    assertThat(slugGenerator.generate("Toto je český žlutý koníček", 3, 6) { true })
      .isEqualTo("toto-j")
  }

  @Test
  fun addsNumbersAndShorten() {
    val callback = { it: String ->
      when (it) {
        "hello-w" -> false
        "hello1" -> false
        "hello2" -> false
        else -> true
      }
    }

    assertThat(slugGenerator.generate("Hello world", 3, 7, callback)).isEqualTo("hello3")
  }

  @Test
  fun shortensAfterNumbers() {
    val callback = { slug: String ->
      (0..9).map { "hello$it" to false }
        .toMap()
        .toMutableMap()
        .also { it["hello-w"] = false }[slug] != false
    }

    assertThat(slugGenerator.generate("Hello world", 3, 7, callback)).isEqualTo("hello10")
  }

  @Test
  fun removesSpecialChars() {
    assertThat(slugGenerator.generate("+--%%\\---normál|____!!`", 3, 50, { true }))
      .isEqualTo("normal")
  }
}
