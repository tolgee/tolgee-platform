package io.tolgee.ee.component.slackIntegration.notification

import com.slack.api.model.Attachment

/** Slack rejects a message with more attachments with the `too_many_attachments` error. */
const val MAX_SLACK_MESSAGE_ATTACHMENTS = 100

fun sortSlackMessageAttachments(attachments: MutableList<Attachment>): MutableList<Attachment> {
  fun getLanguageName(attachment: Attachment): String {
    val textBlock = attachment.blocks[0].toString()
    // Assuming the language name is surrounded by asterisks (e.g., 🇸🇬 *Chinese*)
    return textBlock.substringAfter('*').substringBefore('*').trim()
  }

  val baseLanguageAttachmentIndex = attachments.indexOfFirst { isBaseLanguageAttachment(it) }
  val baseLanguageAttachment =
    if (baseLanguageAttachmentIndex != -1) {
      attachments.removeAt(baseLanguageAttachmentIndex)
    } else {
      null
    }

  val buttonAttachment = attachments.removeAt(attachments.size - 1)

  attachments.sortBy { getLanguageName(it) }

  baseLanguageAttachment?.let {
    attachments.add(0, it)
  }

  attachments.add(buttonAttachment)

  return attachments
}

/**
 * Sorts the attachments and makes sure the message fits into [MAX_SLACK_MESSAGE_ATTACHMENTS].
 *
 * The last attachment has to be the redirect button, which stays last. When the languages don't fit,
 * the base language and then the languages closest to the start of [attachments] are shown,
 * followed by the attachment created by [createHiddenLanguagesAttachment].
 */
fun sortAndLimitSlackMessageAttachments(
  attachments: List<Attachment>,
  createHiddenLanguagesAttachment: (hiddenCount: Int) -> Attachment,
): List<Attachment> {
  if (attachments.size <= MAX_SLACK_MESSAGE_ATTACHMENTS) {
    return sortSlackMessageAttachments(attachments.toMutableList())
  }

  val languageAttachments = attachments.dropLast(1)
  val shownLanguageAttachments =
    languageAttachments
      .sortedByDescending { isBaseLanguageAttachment(it) }
      // one place is taken by the redirect button and one by the hidden languages attachment
      .take(MAX_SLACK_MESSAGE_ATTACHMENTS - 2)

  val sorted = sortSlackMessageAttachments((shownLanguageAttachments + attachments.last()).toMutableList())
  val hiddenCount = languageAttachments.size - shownLanguageAttachments.size
  sorted.add(sorted.lastIndex, createHiddenLanguagesAttachment(hiddenCount))
  return sorted
}

private fun isBaseLanguageAttachment(attachment: Attachment) = attachment.blocks[0].toString().contains("(base)")
