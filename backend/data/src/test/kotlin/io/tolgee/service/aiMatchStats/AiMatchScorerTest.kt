package io.tolgee.service.aiMatchStats

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class AiMatchScorerTest {
  @Test
  fun `identical text scores 100`() {
    AiMatchScorer.score("the quick brown fox", "the quick brown fox").assert.isEqualTo(100)
  }

  @Test
  fun `case and whitespace differences still score 100`() {
    AiMatchScorer.score("The  Quick   Brown Fox", " the quick brown fox ").assert.isEqualTo(100)
  }

  @Test
  fun `one changed token of five scores 80`() {
    AiMatchScorer.score("the quick brown fox jumps", "the quick brown cat jumps").assert.isEqualTo(80)
  }

  @Test
  fun `a single change in a long string never rounds up to 100 - capped at 99`() {
    val ai = (1..200).joinToString(" ") { "w$it" }
    val final = ai.replaceFirst("w1 ", "x1 ")
    AiMatchScorer.score(ai, final).assert.isEqualTo(99)
  }

  @Test
  fun `completely different text scores 0`() {
    AiMatchScorer.score("alpha beta gamma", "one two three").assert.isEqualTo(0)
  }

  @Test
  fun `extra token halves the score`() {
    AiMatchScorer.score("hello", "hello world").assert.isEqualTo(50)
  }

  @Test
  fun `both empty scores 100`() {
    AiMatchScorer.score("", "   ").assert.isEqualTo(100)
  }
}
