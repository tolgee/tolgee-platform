package io.tolgee.unit

import io.tolgee.activity.iterceptor.PreCommitEventPublisher
import io.tolgee.events.OnEntityPreDelete
import io.tolgee.events.OnEntityPrePersist
import io.tolgee.events.OnEntityPreUpdate
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.testing.assert
import io.tolgee.util.getWordUsageIncreaseAmount
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class WordUsageIncreaseAmountTest {
  private val source = mock<PreCommitEventPublisher>()

  @Test
  fun `a created translation adds its words`() {
    OnEntityPrePersist(source, translation(wordCount = 7))
      .getWordUsageIncreaseAmount()
      .assert
      .isEqualTo(7)
  }

  @Test
  fun `a created translation with no word count adds nothing`() {
    OnEntityPrePersist(source, translation(wordCount = null))
      .getWordUsageIncreaseAmount()
      .assert
      .isEqualTo(0)
  }

  @Test
  fun `a deleted translation removes its words`() {
    OnEntityPreDelete(source, translation(wordCount = 7))
      .getWordUsageIncreaseAmount()
      .assert
      .isEqualTo(-7)
  }

  @Test
  fun `an edit reports the difference between the old and the new word count`() {
    update(newWordCount = 10, previousWordCount = 4)
      .getWordUsageIncreaseAmount()
      .assert
      .isEqualTo(6)
  }

  @Test
  fun `an edit that shortens the text reports a negative difference`() {
    update(newWordCount = 4, previousWordCount = 10)
      .getWordUsageIncreaseAmount()
      .assert
      .isEqualTo(-6)
  }

  @Test
  fun `an edit whose previous word count was null counts the whole new value`() {
    update(newWordCount = 10, previousWordCount = null)
      .getWordUsageIncreaseAmount()
      .assert
      .isEqualTo(10)
  }

  @Test
  fun `an update that does not touch the word count reports nothing`() {
    OnEntityPreUpdate(
      source,
      translation(wordCount = 10),
      arrayOf<Any>("some text"),
      arrayOf("text"),
    ).getWordUsageIncreaseAmount()
      .assert
      .isEqualTo(0)
  }

  @Test
  fun `an update with no loaded previous state reports nothing`() {
    OnEntityPreUpdate(source, translation(wordCount = 10), null, arrayOf("wordCount"))
      .getWordUsageIncreaseAmount()
      .assert
      .isEqualTo(0)
  }

  @Test
  fun `an event about anything other than a translation reports nothing`() {
    OnEntityPrePersist(source, Key())
      .getWordUsageIncreaseAmount()
      .assert
      .isEqualTo(0)
  }

  private fun update(
    newWordCount: Int,
    previousWordCount: Int?,
  ): OnEntityPreUpdate<Translation> =
    OnEntityPreUpdate(
      source,
      translation(wordCount = newWordCount),
      arrayOf(previousWordCount ?: NULL_PLACEHOLDER),
      arrayOf("wordCount"),
    )

  private fun translation(wordCount: Int?): Translation =
    Translation("some text").apply {
      this.wordCount = wordCount
    }

  companion object {
    /**
     * Hibernate hands the previous state through as `Array<out Any>`, so a previously-null column
     * arrives as a non-Int entry rather than a hole in the array.
     */
    private val NULL_PLACEHOLDER = Any()
  }
}
