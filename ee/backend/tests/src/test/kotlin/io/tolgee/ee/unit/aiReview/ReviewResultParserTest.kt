package io.tolgee.ee.unit.aiReview

import io.tolgee.ee.service.aiReview.scoring.ReviewParseException
import io.tolgee.ee.service.aiReview.scoring.ReviewResultParser
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

class ReviewResultParserTest {
  private val objectMapper = jacksonObjectMapper()

  private fun parse(response: String) = ReviewResultParser.parse(response, objectMapper)

  private fun points(
    pointsJson: String,
    severity: String = "major",
  ) = parse("""{"issues": [{"severity": "$severity", "message": "m", "points": $pointsJson}]}""")
    .single()
    .points

  private fun pointsWithoutField(severity: String) =
    parse("""{"issues": [{"severity": "$severity", "message": "m"}]}""").single().points

  @Test
  fun `parses plain json`() {
    parse("""{"issues": [{"severity": "major", "category": "accuracy", "message": "Wrong meaning", "points": 35}]}""")
      .assert
      .containsExactly(AiReviewIssue(severity = "major", category = "accuracy", message = "Wrong meaning", points = 35))
  }

  @Test
  fun `parses json inside a fenced block`() {
    val json = """{"issues": [{"severity": "minor", "message": "m", "points": 5}]}"""
    val response = "Here you go:\n```json\n$json\n```"
    parse(response)
      .single()
      .points.assert
      .isEqualTo(5)
  }

  @Test
  fun `parses json with text before it`() {
    parse("""Sure! {"issues": []}""").assert.isEmpty()
  }

  @Test
  fun `empty issues list`() {
    parse("""{"issues": []}""").assert.isEmpty()
  }

  @Test
  fun `missing issues fails`() {
    assertThatThrownBy { parse("""{"output": "x"}""") }.isInstanceOf(ReviewParseException::class.java)
  }

  @Test
  fun `not json fails`() {
    assertThatThrownBy { parse("I cannot review this.") }.isInstanceOf(ReviewParseException::class.java)
  }

  @Test
  fun `issue without message fails`() {
    assertThatThrownBy { parse("""{"issues": [{"severity": "major", "points": 30}]}""") }
      .isInstanceOf(ReviewParseException::class.java)
  }

  @Test
  fun `empty severity fails`() {
    assertThatThrownBy { parse("""{"issues": [{"severity": "", "message": "m", "points": 30}]}""") }
      .isInstanceOf(ReviewParseException::class.java)
  }

  @Test
  fun `custom severity is kept as it is`() {
    parse("""{"issues": [{"severity": "Blocker", "message": "m", "points": 60}]}""")
      .single()
      .severity.assert
      .isEqualTo("Blocker")
  }

  @Test
  fun `missing category is null`() {
    parse("""{"issues": [{"severity": "minor", "message": "m", "points": 5}]}""")
      .single()
      .category.assert
      .isNull()
  }

  @Test
  fun `missing points of a default severity use the upper edge`() {
    pointsWithoutField("Major").assert.isEqualTo(50)
    pointsWithoutField("critical").assert.isEqualTo(100)
    pointsWithoutField("MINOR").assert.isEqualTo(15)
  }

  @Test
  fun `missing points of an unknown severity use 100`() {
    pointsWithoutField("blocker").assert.isEqualTo(100)
  }

  @Test
  fun `negative points use the fallback`() {
    points("-5").assert.isEqualTo(50)
  }

  @Test
  fun `numeric string points are a number`() {
    points("\"35\"").assert.isEqualTo(35)
  }

  @Test
  fun `decimal points are rounded half up`() {
    points("35.5").assert.isEqualTo(36)
    points("\"35.5\"").assert.isEqualTo(36)
  }

  @Test
  fun `non-numeric points use the fallback`() {
    points("\"abc\"").assert.isEqualTo(50)
  }

  @Test
  fun `points are not clamped`() {
    points("300").assert.isEqualTo(300)
  }
}
