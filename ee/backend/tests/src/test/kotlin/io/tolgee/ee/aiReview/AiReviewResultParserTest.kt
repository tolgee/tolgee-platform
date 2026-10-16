package io.tolgee.ee.aiReview

import io.tolgee.ee.service.aiReview.AiReviewResultParser
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.enums.aiReview.AiReviewIssueSeverity
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

class AiReviewResultParserTest {
  private val parser = AiReviewResultParser(jacksonObjectMapper())

  private fun parse(response: String) = parser.parse(response)

  private fun assertInvalid(response: String) {
    assertThatThrownBy { parse(response) }.isInstanceOf(AiReviewResultParser.InvalidOutput::class.java)
  }

  @Test
  fun `parses valid output`() {
    val result =
      parse(
        """
        {"issues":[
          {"severity":"major","category":"grammar","message":"Wrong case","suggestion":"Jetzt zahlen","points":25},
          {"severity":"minor","category":"punctuation","message":"Missing dot","suggestion":null,"points":5}
        ]}
        """.trimIndent(),
      )
    result.score.assert.isEqualTo(70)
    result.issues.assert.containsExactly(
      AiReviewIssue(AiReviewIssueSeverity.MAJOR, "grammar", "Wrong case", 25),
      AiReviewIssue(AiReviewIssueSeverity.MINOR, "punctuation", "Missing dot", 5),
    )
  }

  @Test
  fun `unwraps review output wrapped in the translation schema`() {
    val result = parse("""{"output":"{\"issues\":[]}","contextDescription":"Translation appears correct"}""")
    result.score.assert.isEqualTo(100)
    result.issues.assert.isEmpty()
  }

  @Test
  fun `unwraps issues wrapped in the translation schema`() {
    val result =
      parse(
        """{"output":"{\"issues\":[{\"severity\":\"minor\",\"message\":\"Filler word\",\"points\":5}]}",""" +
          """"contextDescription":"One nit"}""",
      )
    result.score.assert.isEqualTo(95)
    result.issues
      .single()
      .message.assert
      .isEqualTo("Filler word")
  }

  @Test
  fun `rejects translation schema output without issues`() {
    assertInvalid("""{"output":"Jetzt zahlen","contextDescription":"Translated"}""")
  }

  @Test
  fun `parses output in a code fence`() {
    val result =
      parse(
        """
        ```json
        {"issues":[{"severity":"minor","message":"Nit","points":5}]}
        ```
        """.trimIndent(),
      )
    result.score.assert.isEqualTo(95)
  }

  @Test
  fun `parses output surrounded by prose`() {
    val result =
      parse("""Here is my review: {"issues":[{"severity":"critical","message":"Wrong meaning"}]} Hope it helps.""")
    result.score.assert.isEqualTo(25)
    result.issues
      .single()
      .category.assert
      .isEqualTo("other")
  }

  @Test
  fun `empty issue list scores 100`() {
    val result = parse("""{"issues":[]}""")
    result.score.assert.isEqualTo(100)
    result.issues.assert.isEmpty()
  }

  @Test
  fun `score is clamped to zero`() {
    val result =
      parse(
        """{"issues":[{"severity":"critical","message":"a","points":75},{"severity":"critical","message":"b","points":75}]}""",
      )
    result.score.assert.isEqualTo(0)
  }

  @Test
  fun `points outside the severity range are clamped into it`() {
    val result =
      parse(
        """{"issues":[""" +
          """{"severity":"minor","message":"a","points":-30},""" +
          """{"severity":"major","message":"b","points":90},""" +
          """{"severity":"critical","message":"c","points":10}""" +
          """]}""",
      )
    result.issues
      .map { it.points }
      .assert
      .containsExactly(5, 50, 75)
    result.score.assert.isEqualTo(0)
  }

  @Test
  fun `points within the severity range are kept`() {
    val result =
      parse(
        """{"issues":[{"severity":"major","message":"a","points":40},{"severity":"minor","message":"b","points":12}]}""",
      )
    result.issues
      .map { it.points }
      .assert
      .containsExactly(40, 12)
    result.score.assert.isEqualTo(48)
  }

  @Test
  fun `missing points use the lowest points of the severity`() {
    val result =
      parse(
        """{"issues":[{"severity":"MAJOR","message":"a"},{"severity":"Minor","message":"b"}]}""",
      )
    result.issues
      .map { it.points }
      .assert
      .containsExactly(25, 5)
    result.score.assert.isEqualTo(70)
  }

  @Test
  fun `float and string points are accepted`() {
    val result =
      parse(
        """{"issues":[{"severity":"minor","message":"a","points":7.6},{"severity":"minor","message":"b","points":"12"}]}""",
      )
    result.issues
      .map { it.points }
      .assert
      .containsExactly(8, 12)
    result.score.assert.isEqualTo(80)
  }

  @Test
  fun `long texts are truncated`() {
    val result =
      parse("""{"issues":[{"severity":"minor","category":"${"c".repeat(300)}","message":"${"m".repeat(3000)}"}]}""")
    val issue = result.issues.single()
    issue.category.length.assert
      .isEqualTo(100)
    issue.message.length.assert
      .isEqualTo(2000)
  }

  @Test
  fun `rejects unknown severity`() {
    assertInvalid("""{"issues":[{"severity":"blocker","message":"a"}]}""")
  }

  @Test
  fun `rejects missing issues`() {
    assertInvalid("""{"output":"Hallo"}""")
  }

  @Test
  fun `rejects non-array issues`() {
    assertInvalid("""{"issues":"none"}""")
  }

  @Test
  fun `rejects blank message`() {
    assertInvalid("""{"issues":[{"severity":"minor","message":"  "}]}""")
  }

  @Test
  fun `rejects non-object issue`() {
    assertInvalid("""{"issues":["bad"]}""")
  }

  @Test
  fun `rejects garbage`() {
    assertInvalid("not a json")
  }
}
