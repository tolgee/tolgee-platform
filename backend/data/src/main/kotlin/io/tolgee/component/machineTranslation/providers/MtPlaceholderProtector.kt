package io.tolgee.component.machineTranslation.providers

/**
 * Shields Tolgee's ICU-parameter placeholders (`{xxNxx}` sentinels produced by
 * [io.tolgee.helpers.TextHelper.replaceIcuParams]) from being altered by an MT engine.
 *
 * The translated text is matched back to the original placeholders by exact string, so a provider
 * that rewrites a sentinel (e.g. AWS Translate "translating" `{xx1xx}` into CJK) breaks restoration
 * and leaks a broken placeholder. Providers whose engine needs an engine-specific escaping mechanism
 * override [io.tolgee.component.machineTranslation.providers.AbstractMtValueProvider.placeholderProtector].
 */
interface MtPlaceholderProtector {
  /** Wraps placeholders so the engine leaves them untouched. Called on the text sent to the engine. */
  fun protect(text: String): String

  /** Removes the protection wrapping. Called on the text returned by the engine. */
  fun restore(text: String): String
}

object NoopMtPlaceholderProtector : MtPlaceholderProtector {
  override fun protect(text: String): String = text

  override fun restore(text: String): String = text
}
