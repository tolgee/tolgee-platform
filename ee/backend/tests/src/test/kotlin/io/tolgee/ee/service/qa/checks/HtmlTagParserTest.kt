package io.tolgee.ee.service.qa.checks

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HtmlTagParserTest {
  @Test
  fun `returns empty for text without tags`() {
    assertThat(HtmlTagParser.findTags("Hello world")).isEmpty()
  }

  @Test
  fun `returns empty for empty text`() {
    assertThat(HtmlTagParser.findTags("")).isEmpty()
  }

  @Test
  fun `extracts simple open tag`() {
    val tags = HtmlTagParser.findTags("<b>")
    assertThat(tags).hasSize(1)
    assertThat(tags[0].name).isEqualTo("b")
    assertThat(tags[0].kind).isEqualTo(HtmlTagKind.OPEN)
    assertThat(tags[0].raw).isEqualTo("<b>")
    assertThat(tags[0].start).isEqualTo(0)
    assertThat(tags[0].end).isEqualTo(3)
  }

  @Test
  fun `extracts close tag`() {
    val tags = HtmlTagParser.findTags("</b>")
    assertThat(tags).hasSize(1)
    assertThat(tags[0].name).isEqualTo("b")
    assertThat(tags[0].kind).isEqualTo(HtmlTagKind.CLOSE)
    assertThat(tags[0].raw).isEqualTo("</b>")
  }

  @Test
  fun `extracts self-closing tag with slash`() {
    val tags = HtmlTagParser.findTags("<br/>")
    assertThat(tags).hasSize(1)
    assertThat(tags[0].name).isEqualTo("br")
    assertThat(tags[0].kind).isEqualTo(HtmlTagKind.SELF_CLOSING)
  }

  @Test
  fun `extracts self-closing tag with space before slash`() {
    val tags = HtmlTagParser.findTags("<br />")
    assertThat(tags).hasSize(1)
    assertThat(tags[0].name).isEqualTo("br")
    assertThat(tags[0].kind).isEqualTo(HtmlTagKind.SELF_CLOSING)
  }

  @Test
  fun `extracts tag with attributes`() {
    val tags = HtmlTagParser.findTags("""<a href="https://example.com">""")
    assertThat(tags).hasSize(1)
    assertThat(tags[0].name).isEqualTo("a")
    assertThat(tags[0].kind).isEqualTo(HtmlTagKind.OPEN)
  }

  @Test
  fun `extracts tag with single-quoted attribute`() {
    val tags = HtmlTagParser.findTags("<font color='red'>")
    assertThat(tags).hasSize(1)
    assertThat(tags[0].name).isEqualTo("font")
    assertThat(tags[0].kind).isEqualTo(HtmlTagKind.OPEN)
  }

  @Test
  fun `extracts tag with multiple attributes`() {
    val tags = HtmlTagParser.findTags("""<a href="url" class="link">""")
    assertThat(tags).hasSize(1)
    assertThat(tags[0].name).isEqualTo("a")
  }

  @Test
  fun `extracts tag with boolean attribute`() {
    val tags = HtmlTagParser.findTags("<input disabled>")
    assertThat(tags).hasSize(1)
    assertThat(tags[0].name).isEqualTo("input")
  }

  @Test
  fun `extracts multiple tags from mixed content`() {
    val tags = HtmlTagParser.findTags("Hello <b>world</b> <br/> end")
    assertThat(tags).hasSize(3)
    assertThat(tags[0]).satisfies({
      assertThat(it.name).isEqualTo("b")
      assertThat(it.kind).isEqualTo(HtmlTagKind.OPEN)
      assertThat(it.start).isEqualTo(6)
      assertThat(it.end).isEqualTo(9)
    })
    assertThat(tags[1]).satisfies({
      assertThat(it.name).isEqualTo("b")
      assertThat(it.kind).isEqualTo(HtmlTagKind.CLOSE)
      assertThat(it.start).isEqualTo(14)
      assertThat(it.end).isEqualTo(18)
    })
    assertThat(tags[2]).satisfies({
      assertThat(it.name).isEqualTo("br")
      assertThat(it.kind).isEqualTo(HtmlTagKind.SELF_CLOSING)
    })
  }

  @Test
  fun `handles custom tag names`() {
    val tags = HtmlTagParser.findTags("<myComponent>text</myComponent>")
    assertThat(tags).hasSize(2)
    assertThat(tags[0].name).isEqualTo("myComponent")
    assertThat(tags[0].kind).isEqualTo(HtmlTagKind.OPEN)
    assertThat(tags[1].name).isEqualTo("myComponent")
    assertThat(tags[1].kind).isEqualTo(HtmlTagKind.CLOSE)
  }

  @Test
  fun `handles hyphenated tag names`() {
    val tags = HtmlTagParser.findTags("<my-tag>text</my-tag>")
    assertThat(tags).hasSize(2)
    assertThat(tags[0].name).isEqualTo("my-tag")
  }

  @Test
  fun `handles tag names with dots and underscores`() {
    val tags = HtmlTagParser.findTags("<my_tag.name>text</my_tag.name>")
    assertThat(tags).hasSize(2)
    assertThat(tags[0].name).isEqualTo("my_tag.name")
  }

  @Test
  fun `ignores invalid angle brackets`() {
    val tags = HtmlTagParser.findTags("x < 5 and y > 3")
    assertThat(tags).isEmpty()
  }

  @Test
  fun `ignores HTML comments`() {
    val tags = HtmlTagParser.findTags("<!-- comment -->")
    assertThat(tags).isEmpty()
  }

  @Test
  fun `ignores CDATA`() {
    val tags = HtmlTagParser.findTags("<![CDATA[data]]>")
    assertThat(tags).isEmpty()
  }

  @Test
  fun `handles attribute with angle bracket in value`() {
    val tags = HtmlTagParser.findTags("""<a href="page?x=1&y>2">link</a>""")
    // The > inside the quoted attribute value should be consumed as part of the value
    assertThat(tags).hasSize(2)
    assertThat(tags[0].name).isEqualTo("a")
    assertThat(tags[0].kind).isEqualTo(HtmlTagKind.OPEN)
    assertThat(tags[1].name).isEqualTo("a")
    assertThat(tags[1].kind).isEqualTo(HtmlTagKind.CLOSE)
  }

  @Test
  fun `void element without slash parsed as open`() {
    // The parser extracts tags as-is; void element handling is in HtmlSyntaxCheck
    val tags = HtmlTagParser.findTags("<br>")
    assertThat(tags).hasSize(1)
    assertThat(tags[0].name).isEqualTo("br")
    assertThat(tags[0].kind).isEqualTo(HtmlTagKind.OPEN)
  }

  @Test
  fun `self-closing with attributes`() {
    val tags = HtmlTagParser.findTags("""<img src="photo.jpg" />""")
    assertThat(tags).hasSize(1)
    assertThat(tags[0].name).isEqualTo("img")
    assertThat(tags[0].kind).isEqualTo(HtmlTagKind.SELF_CLOSING)
  }

  @Test
  fun `nested tags with correct positions`() {
    val text = "<div><b>text</b></div>"
    val tags = HtmlTagParser.findTags(text)
    assertThat(tags).hasSize(4)
    assertThat(tags.map { it.raw }).containsExactly("<div>", "<b>", "</b>", "</div>")
    assertThat(tags[0].start).isEqualTo(0)
    assertThat(tags[0].end).isEqualTo(5)
    assertThat(tags[1].start).isEqualTo(5)
    assertThat(tags[1].end).isEqualTo(8)
    assertThat(tags[2].start).isEqualTo(12)
    assertThat(tags[2].end).isEqualTo(16)
    assertThat(tags[3].start).isEqualTo(16)
    assertThat(tags[3].end).isEqualTo(22)
  }
}
