package io.tolgee.unit.formats

import io.tolgee.formats.MessagePatternUtil
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import kotlin.time.measureTime

class MessagePatternUtilTest {
  @Test
  fun `returns correct pattern strings for root node`() {
    val full = "{hello, plural, other {# dogs} one {one dog}}"
    MessagePatternUtil
      .buildMessageNode(full)
      .assertPatternString(full)
  }

  @Test
  fun `returns correct pattern strings simple arg node`() {
    val root = MessagePatternUtil.buildMessageNode("{hello}")
    root.contents
      .single()
      .patternString.assert
      .isEqualTo("{hello}")
  }

  @Test
  fun `returns correct pattern strings with arg node wrapped with text`() {
    val root = MessagePatternUtil.buildMessageNode("Hello, {hello}!")
    root.contents[1]
      .patternString.assert
      .isEqualTo("{hello}")
  }

  @Test
  fun `returns correct pattern strings with arg node with type`() {
    val root = MessagePatternUtil.buildMessageNode("Hello, {hello, number}!")
    root.contents[1]
      .patternString.assert
      .isEqualTo("{hello, number}")
  }

  @Test
  fun `returns correct pattern strings with arg node with style`() {
    MessagePatternUtil
      .buildMessageNode("Hello, {hello, number, scientific}!")
      .contents[1]
      .patternString.assert
      .isEqualTo("{hello, number, scientific}")
    MessagePatternUtil
      .buildMessageNode("Hello, {hello, number, 0.00}!")
      .contents[1]
      .patternString.assert
      .isEqualTo("{hello, number, 0.00}")
  }

  @Test
  fun `returns correct pattern string for plural forms`() {
    val argNode =
      MessagePatternUtil
        .buildMessageNode(
          "Hello, {dogsCount, plural, one {I have one dog} other {I have # dogs}}!",
        ).contents[1] as MessagePatternUtil.ArgNode

    argNode.complexStyle!!.variants.let {
      val one = it[0]
      one.patternString.assert.isEqualTo("I have one dog")
      one.message
        ?.contents!![0]
        .patternString.assert
        .isEqualTo("I have one dog")
      val other = it[1]
      other.patternString.assert.isEqualTo("I have # dogs")
    }
  }

  @Test
  fun `returns correct pattern string for select`() {
    val argNode =
      MessagePatternUtil
        .buildMessageNode(
          "Hello, {gender, select, man {I am a man!} woman {I am a woman!} other {}}!",
        ).contents[1] as MessagePatternUtil.ArgNode

    argNode.complexStyle!!.variants.let {
      val man = it[0]
      man.patternString.assert.isEqualTo("I am a man!")
      val woman = it[1]
      woman.patternString.assert.isEqualTo("I am a woman!")
    }
  }

  @Test
  fun `returns correct pattern string for choice`() {
    val argNode =
      MessagePatternUtil
        .buildMessageNode(
          "Hello, {count, choice, 0#There are no dogs|1#There is one dog|1<There are # dogs}!",
        ).contents[1] as MessagePatternUtil.ArgNode

    argNode.complexStyle!!.variants.let {
      it[0].patternString.assert.isEqualTo("There are no dogs")
      it[1].patternString.assert.isEqualTo("There is one dog")
      it[2].patternString.assert.isEqualTo("There are # dogs")
    }
  }

  @Test
  fun `returns correct pattern string for selectordinal`() {
    val argNode =
      MessagePatternUtil
        .buildMessageNode(
          "Hello, {count, selectordinal, one {#st dog} two {#nd dog} few {#rd dog} other {#th dog}}!",
        ).contents[1] as MessagePatternUtil.ArgNode

    argNode.complexStyle!!.variants.let {
      it[0].patternString.assert.isEqualTo("#st dog")
      it[1].patternString.assert.isEqualTo("#nd dog")
      it[2].patternString.assert.isEqualTo("#rd dog")
      it[3].patternString.assert.isEqualTo("#th dog")
    }
  }

  @Test
  fun `returns correct pattern string for nested plurals`() {
    val nestedTireCountPlural = "{tireCount, plural, one {# car has one tire} other {# car has # tires}}"
    val full =
      "{count, plural, " +
        "one {$nestedTireCountPlural} " +
        "other {$nestedTireCountPlural}" +
        "}"
    val root = MessagePatternUtil.buildMessageNode(full)
    val firstPlural = root.contents.first() as MessagePatternUtil.ArgNode
    val secondPlural = (
      firstPlural.complexStyle!!
        .variants[0]
        .message
        ?.contents
        ?.get(0) as MessagePatternUtil.ArgNode?
    )
    val thirdPlural = (
      firstPlural.complexStyle!!
        .variants[1]
        .message
        ?.contents
        ?.get(0) as MessagePatternUtil.ArgNode?
    )
    firstPlural.patternString.assert.isEqualTo(full)
    secondPlural!!.patternString.assert.isEqualTo(nestedTireCountPlural)
    thirdPlural!!.patternString.assert.isEqualTo(nestedTireCountPlural)
  }

  @Test
  fun `returns correct pattern string for escape sequences`() {
    val simple =
      MessagePatternUtil.buildMessageNode(
        "'{'Hello'}' {name}.",
      )
    simple.contents[0]
      .patternString.assert
      .isEqualTo("'{'Hello'}' ")
    simple.contents[1]
      .patternString.assert
      .isEqualTo("{name}")

    val comlpex =
      MessagePatternUtil.buildMessageNode(
        "'{Hello}' {name} '{'Lala'}' '{Lala ' '",
      )
    comlpex.contents[0]
      .patternString.assert
      .isEqualTo("'{Hello}' ")
    comlpex.contents[1]
      .patternString.assert
      .isEqualTo("{name}")
    comlpex.contents[2]
      .patternString.assert
      .isEqualTo(" '{'Lala'}' '{Lala ' '")

    MessagePatternUtil
      .buildMessageNode(
        "'{ ' ",
      ).contents[0]
      .patternString.assert
      .isEqualTo("'{ ' ")

    MessagePatternUtil
      .buildMessageNode(
        "'{ '",
      ).contents[0]
      .patternString.assert
      .isEqualTo("'{ '")

    MessagePatternUtil
      .buildMessageNode(
        " ' ",
      ).contents[0]
      .patternString.assert
      .isEqualTo(" ' ")
  }

  @Test
  fun `returns correct pattern string when string ends with escape char`() {
    MessagePatternUtil
      .buildMessageNode(
        " '{hello}'",
      ).contents[0]
      .patternString.assert
      .isEqualTo(" '{hello}'")
  }

  @Test
  fun `returns correct pattern when multiplied escape chars`() {
    MessagePatternUtil
      .buildMessageNode(
        "'''{'''",
      ).contents[0]
      .patternString.assert
      .isEqualTo("'''{'''")
  }

  @Test
  fun `returns correct pattern for four escape chars`() {
    MessagePatternUtil
      .buildMessageNode(
        "''''",
      ).contents[0]
      .patternString.assert
      .isEqualTo("''''")
  }

  @Test
  fun `returns for quotes in escape sequence`() {
    val contents =
      MessagePatternUtil
        .buildMessageNode(
          "Hey '{ '''This is it'.",
        ).contents[0]
    contents.patternString.assert.isEqualTo("Hey '{ '''This is it'.")
  }

  @Test
  fun `returns correct pattern string for complex plurals`() {
    val root =
      MessagePatternUtil.buildMessageNode(
        "{count, plural, offset:1 " +
          "=0 {No books} " +
          "one {# book (and one other)} " +
          "other {# books (and # others)}}" +
          "}",
      )
    val firstPlural = root.contents.first() as MessagePatternUtil.ArgNode
    firstPlural.complexStyle!!
      .variants[0]
      .patternString.assert
      .isEqualTo("No books")
    firstPlural.complexStyle!!
      .variants[1]
      .patternString.assert
      .isEqualTo("# book (and one other)")
    firstPlural.complexStyle!!
      .variants[2]
      .patternString.assert
      .isEqualTo("# books (and # others)")
  }

  @Test
  fun `performs well`() {
    measureTime {
      (0..20000).forEach {
        val string =
          "{gender, select, " +
            "male {" +
            "{age, plural, " +
            "one {{count, plural, one {He is 1 year old and has 1 cat} " +
            "other {He is 1 year old and has # cats}}} " +
            "other {{count, plural, " +
            "one {He is # years old and has 1 cat} " +
            "other {He is # years old and has # cats}}}}} " +
            "female {" +
            "{age, plural, " +
            "one {{count, plural, " +
            "one {She is 1 year old and has 1 cat} " +
            "other {She is 1 year old and has # cats}}} " +
            "other {{count, plural, " +
            "one {She is # years old and has 1 cat} " +
            "other {She is # years old and has # cats}}}}} " +
            "other {{age, plural, " +
            "one {{count, plural, " +
            "one {They are 1 year old and have 1 cat} " +
            "other {They are 1 year old and have # cats}}} " +
            "other {{count, plural, " +
            "one {They are # years old and have 1 cat} " +
            "other {They are # years old and have # cats}}}}}" +
            "}"
        val root = MessagePatternUtil.buildMessageNode(string)
        root.contents.forEach {
          (it as? MessagePatternUtil.ArgNode)?.complexStyle?.variants?.forEach {
            it.patternString
          }
        }
      }
    }.inWholeSeconds.assert.isLessThan(2)

    measureTime {
      (0..1000000).forEach {
        val root = MessagePatternUtil.buildMessageNode("Hello!")
        root.contents.single().patternString
      }
    }.inWholeMilliseconds.assert.isLessThan(500)
  }

  private fun MessagePatternUtil.Node.assertPatternString(expected: String) {
    val patternString = this.patternString
    patternString.assert.isEqualTo(expected)
  }
}
