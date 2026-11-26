package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.translation.Translation

/**
 * Real-world translations from https://github.com/tolgee/tolgee-platform/issues/4080: correct ones
 * that QA checks used to report, and broken ones that must stay reported.
 */
class QaLanguageAwareChecksTestData : BaseTestData() {
  val translations = mutableMapOf<Pair<String, String>, Translation>()

  init {
    project.useQaChecks = true
    projectBuilder.build {
      LANGUAGE_TAGS.forEach { languageTag ->
        addLanguage {
          name = languageTag
          tag = languageTag
          originalName = languageTag
        }
      }
      setQaConfig {
        settings = QaCheckType.entries.associateWith(::severityOf).toMutableMap()
      }
    }

    addPluralKey(
      "n-words-highlighted",
      "{count, plural, one {# word highlighted} other {# words highlighted}}",
      "pl" to
        "{count, plural, one {Zaznaczono # słowo} few {Zaznaczono # słowa} " +
        "many {Zaznaczono # słów} other {Zaznaczono # słowa}}",
      "fr" to "{count, plural, one {# mot surligné} many {# mots surlignés} other {# mots surlignés}}",
    )
    addKey("rows", "rows", "de" to "Zeilen")
    addKey("show", "Show", "de" to "anzeigen")
    addKey("allow-uploads", "Allow iManage uploads", "tr" to "iManage yüklemelerine izin ver")
    addKey("index-download", "index.html download", "fr" to "Télécharger index.html")
    addKey("placeholder", "e.g. Only retail banking.", "nl" to "Bijv. Alleen retailbankieren.")
    addKey(
      "enter-email",
      "Enter an email address",
      "ja" to "email アドレスを入力してください",
      "ko" to "email 주소를 입력하세요",
      "ar" to "email أدخل عنوان البريد الإلكتروني",
      "he" to "email הזן כתובת אימייל",
    )
    addKey("settings-optional", "Settings (optional)", "zh-Hans" to "设置（可选）", "ko" to "설정(선택)")
    addKey("nda", "Non-disclosure agreement", "zh-Hans" to "保密协议（NDA）", "fr" to "Accord de confidentialité (NDA)")
    addPluralKey(
      "selected-items",
      "{count, plural, one {# item selected} other {# items selected}}",
      "nl" to "{count, plural, one {{count} item geselecteerd} other {{count} items geselecteerd}}",
    )
    addKey("is-after", "is after", "sv" to "Är efter")
    addPluralKey(
      "documents-count",
      "{count, plural, one {# document} other {# documents}}",
      "ar" to
        "{count, plural, zero {لا توجد مستندات} one {مستند واحد} two {مستندان} " +
        "few {# مستندات} many {# مستندًا} other {# مستند}}",
      "he" to "{count, plural, one {מסמך אחד} two {שני מסמכים} other {# מסמכים}}",
    )
    addPluralKey(
      "documents-count-broken",
      "{count, plural, one {# document} other {# documents}}",
      "ar" to
        "{count, plural, zero {لا توجد مستندات} one {مستند واحد} two {مستندان} " +
        "few {ثلاثة مستندات} many {أحد عشر مستندًا} other {# مستند}}",
    )
    addPluralKey(
      "pending-edits",
      "{count, plural, one {{count} edit} other {{count} edits}}",
      "nl" to "{count, plural, one {# bewerking} other {# bewerkingen}}",
    )
    addPluralKey(
      "collections",
      "{count, plural, one {1 Collection} other {# Collections}}",
      "sv" to "{count, plural, one {# samling} other {# samlingar}}",
    )
    addPluralKey(
      "all-files",
      "{count, plural, one {# file} other {All # files}}",
      "es" to "{count, plural, one {# archivo} many {Todos los archivos} other {Todos los archivos}}",
    )
    addKey(
      "not-a-member",
      "You are not a member of any organization.",
      "ja" to "どの組織にも所属していません。",
    )
    addKey(
      "are-you-sure",
      "Are you sure?",
      "ar" to "هل أنت متأكد؟",
      "ja" to "よろしいですか？",
      "el" to "Είστε σίγουροι;",
    )
    addKey("saved", "Saved", "fr" to "Enregistré.", "ja" to "保存しました。")
    addKey(
      "toggle-department",
      "Failed to toggle department ID (Client ID) requirement",
      "ja" to "部門 ID （クライアント ID）の必須設定 の切り替えに失敗",
    )
    addKey("partner", "Partner", "fr" to "Associé(e)")
    addKey(
      "user-updated-field",
      "<name></name> updated <field></field>",
      "ko" to "<name></name> 가 <field></field> 을(를) 업데이트했습니다",
    )
    addKey(
      "nda-guidelines",
      "Acme NDA Guidelines",
      "ar" to "إرشادات Acme لاتفاقية عدم الإفصاح (NDA)",
    )
    addKey("hello-world", "Hello (world)", "fr" to "Bonjour (monde", "ja" to "こんにちは（世界")
  }

  private fun severityOf(type: QaCheckType): QaCheckSeverity {
    if (type in LANGUAGE_AWARE_CHECKS) return QaCheckSeverity.WARNING
    return QaCheckSeverity.OFF
  }

  private fun addKey(
    name: String,
    base: String,
    vararg translations: Pair<String, String>,
  ) = addKey(name, base, plural = false, translations)

  private fun addPluralKey(
    name: String,
    base: String,
    vararg translations: Pair<String, String>,
  ) = addKey(name, base, plural = true, translations)

  private fun addKey(
    name: String,
    base: String,
    plural: Boolean,
    translations: Array<out Pair<String, String>>,
  ) {
    projectBuilder
      .addKey {
        this.name = name
        isPlural = plural
        if (plural) pluralArgName = "count"
      }.build {
        addTranslation("en", base)
        translations.forEach { (languageTag, text) ->
          this@QaLanguageAwareChecksTestData.translations[name to languageTag] = addTranslation(languageTag, text).self
        }
      }
  }

  companion object {
    private val LANGUAGE_TAGS =
      listOf("pl", "fr", "de", "tr", "nl", "ja", "ko", "ar", "he", "sv", "es", "zh-Hans", "el")

    val LANGUAGE_AWARE_CHECKS =
      setOf(
        QaCheckType.CHARACTER_CASE_MISMATCH,
        QaCheckType.INCONSISTENT_PLACEHOLDERS,
        QaCheckType.PUNCTUATION_MISMATCH,
        QaCheckType.BRACKETS_MISMATCH,
      )
  }
}
