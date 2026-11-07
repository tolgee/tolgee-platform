package io.tolgee.ee.slack

import com.slack.api.model.Attachment
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import io.tolgee.ee.component.slackIntegration.notification.MAX_SLACK_MESSAGE_ATTACHMENTS
import io.tolgee.ee.component.slackIntegration.notification.sortAndLimitSlackMessageAttachments
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class SortSlackMessageAttachmentsTest {
  private val button = attachment("button")
  private val hiddenLanguages = attachment("hidden")

  @Test
  fun `sorts without hiding anything when the attachments fit the limit`() {
    val languages = languageAttachments(MAX_SLACK_MESSAGE_ATTACHMENTS - 1).reversed()

    val result = sortAndLimit(languages + button)

    result.assert.hasSize(MAX_SLACK_MESSAGE_ATTACHMENTS)
    result.assert.doesNotContain(hiddenLanguages)
    result.dropLast(1).assert.isEqualTo(languages.reversed())
    result.last().assert.isEqualTo(button)
  }

  @Test
  fun `hides the last languages and reports their count when over the limit`() {
    val languages = languageAttachments(MAX_SLACK_MESSAGE_ATTACHMENTS)

    var reportedCount: Int? = null
    val result =
      sortAndLimitSlackMessageAttachments(languages + button) {
        reportedCount = it
        hiddenLanguages
      }

    result.assert.hasSize(MAX_SLACK_MESSAGE_ATTACHMENTS)
    result.dropLast(2).assert.isEqualTo(languages.take(MAX_SLACK_MESSAGE_ATTACHMENTS - 2))
    result[result.lastIndex - 1].assert.isEqualTo(hiddenLanguages)
    result.last().assert.isEqualTo(button)
    reportedCount.assert.isEqualTo(2)
  }

  @Test
  fun `shows the base language first even if it is the last one when over the limit`() {
    val languages = languageAttachments(MAX_SLACK_MESSAGE_ATTACHMENTS)
    val base = attachment("*Zulu* (base)")

    val result = sortAndLimit(languages + base + button)

    result.assert.hasSize(MAX_SLACK_MESSAGE_ATTACHMENTS)
    result.first().assert.isEqualTo(base)
  }

  private fun sortAndLimit(attachments: List<Attachment>) =
    sortAndLimitSlackMessageAttachments(attachments) { hiddenLanguages }

  private fun languageAttachments(count: Int) = (1..count).map { attachment("*Language %03d*".format(it)) }

  private fun attachment(text: String) =
    Attachment
      .builder()
      .blocks(
        listOf(
          SectionBlock
            .builder()
            .text(MarkdownTextObject.builder().text(text).build())
            .build(),
        ),
      ).build()
}
