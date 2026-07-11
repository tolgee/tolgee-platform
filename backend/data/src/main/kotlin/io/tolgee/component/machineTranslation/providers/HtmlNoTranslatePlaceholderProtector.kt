package io.tolgee.component.machineTranslation.providers

import io.tolgee.helpers.TextHelper

/**
 * Wraps placeholders in `<span translate="no">…</span>`. Engines that honor the HTML `translate="no"`
 * attribute (notably AWS Translate) return the wrapped content verbatim and keep the tags in the output,
 * which [restore] then strips. Without this, AWS rewrites the bare `{xxNxx}` sentinel for some targets
 * (e.g. Traditional Chinese), breaking placeholder restoration.
 *
 * docs: https://docs.aws.amazon.com/translate/latest/dg/customizing-translations-tags.html
 */
object HtmlNoTranslatePlaceholderProtector : MtPlaceholderProtector {
  private const val OPEN = "<span translate=\"no\">"
  private const val CLOSE = "</span>"

  // AWS may echo the attribute back without quotes (e.g. `translate=no`), so accept both on restore.
  private val wrappedRegex =
    Regex("<span translate=\"?no\"?>(${TextHelper.paramPlaceholderRegex.pattern})</span>")

  override fun protect(text: String): String =
    TextHelper.paramPlaceholderRegex.replace(text) { "$OPEN${it.value}$CLOSE" }

  override fun restore(text: String): String = wrappedRegex.replace(text) { it.groupValues[1] }
}
