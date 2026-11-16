package io.tolgee.service.notification.digest

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.dtos.response.NotificationKeySampleDto
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.translation.CommentMentionService
import io.tolgee.util.I18n
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils

data class DigestRow(
  val notificationId: Long,
  val projectId: Long,
  val projectName: String,
  val type: NotificationType,
  val count: Int,
  val languageNames: List<String>,
  val branchNames: List<String>,
  val sampleKeys: List<NotificationKeySampleDto> = emptyList(),
)

data class MentionRow(
  val notificationId: Long,
  val projectId: Long,
  val projectName: String,
  val authorName: String?,
  val text: String,
  val mentionedNames: Map<Long, String>,
  val keyId: Long,
  val keyName: String,
  val keyNamespace: String?,
  val languageTag: String,
  val languageName: String,
)

data class ComposedDigest(
  val subject: String,
  val html: String,
)

@Component
class NotificationDigestComposer(
  private val i18n: I18n,
  private val frontendUrlProvider: FrontendUrlProvider,
) {
  fun compose(
    rows: List<DigestRow>,
    entityCap: Int,
    mentions: List<MentionRow> = emptyList(),
  ): ComposedDigest {
    val sections = mutableListOf<String>()
    if (mentions.isNotEmpty()) sections.add(mentions.joinToString("<br/><br/>") { mentionBlock(it) })
    if (rows.isNotEmpty()) sections.add(workSection(rows, entityCap))
    val body = sections.joinToString("<br/><br/><hr/><br/>")
    return ComposedDigest(
      subject = subject(rows, entityCap, mentions),
      html =
        i18n.translate("notifications.email.template", body, frontendUrlProvider.getNotificationSettingsUrl()),
    )
  }

  private fun subject(
    rows: List<DigestRow>,
    entityCap: Int,
    mentions: List<MentionRow>,
  ): String {
    val work = workSubject(rows, entityCap, combined = mentions.isNotEmpty())
    val mention = mentionSubject(mentions) ?: return work ?: i18n.translate("notifications.email.digest.subject")
    if (work == null) return mention
    return i18n.translate("notifications.email.subject.combined", mention, work)
  }

  private fun mentionSubject(mentions: List<MentionRow>): String? {
    if (mentions.isEmpty()) return null
    val authors = mentions.mapNotNull { it.authorName }.distinct()
    if (mentions.size == 1 || authors.size == 1) {
      return i18n.translate("notifications.email.subject.mention", authors.firstOrNull() ?: unknownAuthor())
    }
    return i18n.translate("notifications.email.subject.mentions", mentions.size.toString())
  }

  private fun workSubject(
    rows: List<DigestRow>,
    entityCap: Int,
    combined: Boolean,
  ): String? {
    if (rows.isEmpty()) return null
    val keysAdded = rows.filter { it.type == NotificationType.KEYS_ADDED }
    if (keysAdded.isNotEmpty()) {
      val count = keysAdded.sumOf { it.count }
      val capped = keysAdded.any { it.count >= entityCap }
      if (count == 1 && !capped) return i18n.translate("notifications.email.subject.work.key")
      return i18n.translate("notifications.email.subject.work.keys", count.toString() + "+".takeIf { capped }.orEmpty())
    }
    if (combined) return i18n.translate("notifications.email.subject.work.updates.combined")
    return i18n.translate("notifications.email.subject.work.updates")
  }

  private fun mentionBlock(row: MentionRow): String {
    val author = HtmlUtils.htmlEscape(row.authorName ?: unknownAuthor())
    val header = i18n.translate("notifications.email.mention.header", author)
    val keyName = listOfNotNull(row.keyNamespace?.takeIf { it.isNotBlank() }, row.keyName).joinToString(":")
    val meta =
      i18n.translate(
        "notifications.email.mention.meta",
        HtmlUtils.htmlEscape(row.projectName),
        HtmlUtils.htmlEscape(keyName),
        HtmlUtils.htmlEscape(row.languageName),
      )
    val link = frontendUrlProvider.getKeyCommentsUrl(row.projectId, row.keyId, row.languageTag)
    return "<b>$header</b><br/>${renderCommentText(row.text, row.mentionedNames)}<br/>" +
      "<span style=\"color:#6b7280\">$meta</span><br/>" +
      button(link, i18n.translate("notifications.email.mention.button"))
  }

  fun renderCommentText(
    text: String,
    mentionedNames: Map<Long, String>,
  ): String {
    val result = StringBuilder()
    var lastEnd = 0
    CommentMentionService.MENTION_REGEX.findAll(text).forEach { match ->
      result.append(HtmlUtils.htmlEscape(text.substring(lastEnd, match.range.first)))
      val name = match.groupValues[1].toLongOrNull()?.let { mentionedNames[it] }
      result.append(mentionText(match.value, name))
      lastEnd = match.range.last + 1
    }
    result.append(HtmlUtils.htmlEscape(text.substring(lastEnd)))
    return result.toString()
  }

  private fun mentionText(
    token: String,
    name: String?,
  ): String {
    if (name == null) return HtmlUtils.htmlEscape(token)
    return "<b>@${HtmlUtils.htmlEscape(name)}</b>"
  }

  private fun workSection(
    rows: List<DigestRow>,
    entityCap: Int,
  ): String {
    val projects =
      rows.groupBy { it.projectId }.values.joinToString("<br/><br/>") { projectRows ->
        val first = projectRows.first()
        val link = frontendUrlProvider.getProjectUrl(first.projectId)
        val lines =
          projectRows.sortedBy { it.type.ordinal }.joinToString("<br/>") { line(it, entityCap) }
        """<b><a href="$link">${HtmlUtils.htmlEscape(first.projectName)}</a></b><br/>$lines"""
      }
    return "${i18n.translate("notifications.email.digest.intro")}<br/><br/>$projects"
  }

  private fun line(
    row: DigestRow,
    entityCap: Int,
  ): String {
    val text = lineText(row, entityCap)
    val link = frontendUrlProvider.getNotificationTranslationsUrl(row.projectId, row.notificationId)
    return "${withBranches(text, row.branchNames)} ${button(link, i18n.translate("notifications.email.digest.open"))}"
  }

  private fun lineText(
    row: DigestRow,
    entityCap: Int,
  ): String {
    if (row.type == NotificationType.KEYS_ADDED) return keysAddedText(row, entityCap)
    val languages = HtmlUtils.htmlEscape(row.languageNames.joinToString(", "))
    return i18n.translate("notifications.email.digest.line.${row.type.name}", countText(row, entityCap), languages)
  }

  private fun keysAddedText(
    row: DigestRow,
    entityCap: Int,
  ): String {
    val samples = row.sampleKeys
    val project = HtmlUtils.htmlEscape(row.projectName)
    val capped = row.count >= entityCap
    if (!capped && row.count == 1 && samples.size == 1) {
      return i18n.translate("notifications.email.digest.keys-added.one", keyWithText(samples[0]))
    }
    if (!capped && row.count == 2 && samples.size == 2) {
      return i18n.translate(
        "notifications.email.digest.keys-added.two",
        keyWithText(samples[0]),
        keyWithText(samples[1]),
      )
    }
    if (!capped && row.count == 1) {
      return i18n.translate("notifications.email.digest.keys-added.count-one", project)
    }
    if (samples.size < 2) {
      return i18n.translate("notifications.email.digest.keys-added.count", countText(row, entityCap), project)
    }
    val first = HtmlUtils.htmlEscape(fullKeyName(samples[0]))
    val second = HtmlUtils.htmlEscape(fullKeyName(samples[1]))
    if (capped) {
      return i18n.translate(
        "notifications.email.digest.keys-added.capped",
        countText(row, entityCap),
        project,
        first,
        second,
      )
    }
    return i18n.translate(
      "notifications.email.digest.keys-added.many",
      row.count.toString(),
      project,
      first,
      second,
      (row.count - 2).toString(),
    )
  }

  private fun keyWithText(sample: NotificationKeySampleDto): String {
    val name = HtmlUtils.htmlEscape(fullKeyName(sample))
    val text = sample.baseText?.takeIf { it.isNotBlank() } ?: return name
    return i18n.translate("notifications.email.digest.key-with-text", name, HtmlUtils.htmlEscape(shorten(text)))
  }

  private fun fullKeyName(sample: NotificationKeySampleDto): String =
    listOfNotNull(sample.namespace?.takeIf { it.isNotBlank() }, sample.name).joinToString(":")

  private fun shorten(text: String): String {
    if (text.length <= MAX_BASE_TEXT_LENGTH) return text
    return text.take(MAX_BASE_TEXT_LENGTH).trimEnd() + "…"
  }

  private fun countText(
    row: DigestRow,
    entityCap: Int,
  ): String {
    if (row.count >= entityCap) return i18n.translate("notifications.email.digest.count-capped")
    return row.count.toString()
  }

  private fun withBranches(
    text: String,
    branchNames: List<String>,
  ): String {
    if (branchNames.isEmpty()) return text
    return "$text ${branchesText(branchNames)}"
  }

  private fun branchesText(branchNames: List<String>): String =
    i18n.translate("notifications.email.digest.branches", HtmlUtils.htmlEscape(branchNames.joinToString(", ")))

  private fun unknownAuthor(): String = i18n.translate("notifications.email.mention.unknown-author")

  private fun button(
    href: String,
    label: String,
  ): String =
    """<a href="$href" style="display:inline-block;margin-top:4px;padding:4px 10px;border-radius:4px;""" +
      """background:#ec407a;color:#ffffff;text-decoration:none">$label</a>"""

  companion object {
    private const val MAX_BASE_TEXT_LENGTH = 80
  }
}
