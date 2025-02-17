package io.tolgee.ee.component.slackIntegration.notification

import com.slack.api.model.Attachment

fun sortSlackMessageAttachments(attachments: MutableList<Attachment>): MutableList<Attachment> {
  fun getLanguageName(attachment: Attachment): String {
    val textBlock = attachment.blocks[0].toString()
    // Assuming the language name is surrounded by asterisks (e.g., 🇸🇬 *Chinese*)
    return textBlock.substringAfter('*').substringBefore('*').trim()
  }

  val baseLanguageAttachmentIndex =
    attachments.indexOfFirst {
      it.blocks[0].toString().contains("(base)")
    }
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
