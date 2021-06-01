package io.tolgee.util

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.assertions.Assertions.assertThatThrownBy
import io.tolgee.exceptions.BadRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.annotations.Test

@SpringBootTest
class AddressPartGeneratorTest : AbstractTestNGSpringContextTests() {


    @field:Autowired
    lateinit var addressPartGenerator: AddressPartGenerator

    @Test
    fun testGenerateEmpty() {
        assertThat(addressPartGenerator.generate("", 3, 10) { true }).isEqualTo("a10")
    }

    @Test
    fun testValidInput() {
        assertThat(addressPartGenerator.generate("Hello I am cool Project name", 3, 50) { true })
                .isEqualTo("hello-i-am-cool-project-name")
    }

    @Test
    fun throwsWhenTooHard() {
        assertThatThrownBy {
            addressPartGenerator.generate("Hello I am cool Repository name", 3, 50) { false }
        }.isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun removesAccents() {
        assertThat(addressPartGenerator.generate("Toto je český žlutý koníček", 3, 50) { true })
                .isEqualTo("toto-je-cesky-zluty-konicek")
    }

    @Test
    fun shortenString() {
        assertThat(addressPartGenerator.generate("Toto je český žlutý koníček", 3, 6) { true })
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

        assertThat(addressPartGenerator.generate("Hello world", 3, 7, callback)).isEqualTo("hello3")
    }

    @Test
    fun shortensAfterNumbers() {
        val callback = { addressPart: String ->
            (0..9).map { "hello$it" to false }
                    .toMap()
                    .toMutableMap()
                    .also { it["hello-w"] = false }[addressPart] != false
        }

        assertThat(addressPartGenerator.generate("Hello world", 3, 7, callback)).isEqualTo("hello10")
    }

    @Test
    fun removesSpecialChars() {
        assertThat(addressPartGenerator.generate("+--%%\\---normál|____!!`", 3, 50, { true }))
                .isEqualTo("normal")
    }
}
