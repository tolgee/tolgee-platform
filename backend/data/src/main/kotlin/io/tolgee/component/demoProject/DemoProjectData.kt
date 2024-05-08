package io.tolgee.component.demoProject

import io.tolgee.model.Language

private const val ADD_ITEM_ADD_BUTTON_KEY = "add-item-add-button"
private const val ADD_ITEM_INPUT_PLACEHOLDER_KEY = "add-item-input-placeholder"
private const val DELETE_ITEM_BUTTON_KEY = "delete-item-button"
private const val MENU_ITEM_TRANSLATION_METHODS_KEY = "menu-item-translation-methods"
private const val SEND_VIA_EMAIL_KEY = "send-via-email"
private const val SHARE_BUTTON_KEY = "share-button"
private const val APP_TITLE_KEY = "app-title"

object DemoProjectData {
  val translations =
    mapOf(
      "en" to
        mapOf(
          ADD_ITEM_ADD_BUTTON_KEY to "Add",
          ADD_ITEM_INPUT_PLACEHOLDER_KEY to "New list item",
          DELETE_ITEM_BUTTON_KEY to "Delete",
          MENU_ITEM_TRANSLATION_METHODS_KEY to "Translation methods",
          SEND_VIA_EMAIL_KEY to "Send via e-mail",
          SHARE_BUTTON_KEY to "Share",
          APP_TITLE_KEY to "What to pack",
        ),
      "de" to
        mapOf(
          ADD_ITEM_ADD_BUTTON_KEY to "Einfügen",
          ADD_ITEM_INPUT_PLACEHOLDER_KEY to "Neuer Eintrag",
          DELETE_ITEM_BUTTON_KEY to "Löschen",
          MENU_ITEM_TRANSLATION_METHODS_KEY to "Übersetzungsmethoden",
          SEND_VIA_EMAIL_KEY to "Per Email abschicken",
          SHARE_BUTTON_KEY to "Teilen",
          APP_TITLE_KEY to "Was mitnehmen",
        ),
      "fr" to
        mapOf(
          ADD_ITEM_ADD_BUTTON_KEY to "Ajouter",
          ADD_ITEM_INPUT_PLACEHOLDER_KEY to "Nouvel élément de la liste",
          DELETE_ITEM_BUTTON_KEY to "Supprimer",
          MENU_ITEM_TRANSLATION_METHODS_KEY to "Méthodes de traduction",
          SEND_VIA_EMAIL_KEY to "Envoyer par courriel",
          SHARE_BUTTON_KEY to "Partager",
          APP_TITLE_KEY to "Quoi emballer",
        ),
    )

  val screenshots =
    listOf(
      DemoProjectScreenshotReference(
        keyName = ADD_ITEM_ADD_BUTTON_KEY,
        positions = listOf(DemoProjectScreenshotReferencePosition(1601, 359, 168, 86)),
      ),
      DemoProjectScreenshotReference(
        keyName = ADD_ITEM_INPUT_PLACEHOLDER_KEY,
        positions = listOf(DemoProjectScreenshotReferencePosition(617, 359, 951, 86)),
      ),
      DemoProjectScreenshotReference(
        keyName = DELETE_ITEM_BUTTON_KEY,
        positions =
          listOf(
            DemoProjectScreenshotReferencePosition(1637, 568, 116, 35),
            DemoProjectScreenshotReferencePosition(1637, 637, 116, 35),
            DemoProjectScreenshotReferencePosition(1637, 707, 116, 35),
          ),
      ),
      DemoProjectScreenshotReference(
        keyName = MENU_ITEM_TRANSLATION_METHODS_KEY,
        positions = listOf(DemoProjectScreenshotReferencePosition(564, 27, 290, 60)),
      ),
      DemoProjectScreenshotReference(
        keyName = SEND_VIA_EMAIL_KEY,
        positions = listOf(DemoProjectScreenshotReferencePosition(1439, 995, 331, 86)),
      ),
      DemoProjectScreenshotReference(
        keyName = SHARE_BUTTON_KEY,
        positions = listOf(DemoProjectScreenshotReferencePosition(1202, 995, 204, 86)),
      ),
      DemoProjectScreenshotReference(
        keyName = APP_TITLE_KEY,
        positions = listOf(DemoProjectScreenshotReferencePosition(956, 181, 475, 91)),
      ),
    )

  val inTranslatedState = mapOf("fr" to ADD_ITEM_ADD_BUTTON_KEY)

  val keys = translations.keys

  val languages
    get() =
      listOf(
        Language().apply {
          name = "English"
          tag = "en"
          originalName = "English"
          flagEmoji = "🇬🇧"
        },
        Language().apply {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
          flagEmoji = "🇩🇪"
        },
        Language().apply {
          name = "French"
          tag = "fr"
          originalName = "Français"
          flagEmoji = "🇫🇷"
        },
      )

  val tags =
    mapOf(
      ADD_ITEM_ADD_BUTTON_KEY to listOf("web", "button", "draft"),
      SHARE_BUTTON_KEY to listOf("web", "button"),
      DELETE_ITEM_BUTTON_KEY to listOf("web", "button"),
      MENU_ITEM_TRANSLATION_METHODS_KEY to listOf("web", "menu"),
      SEND_VIA_EMAIL_KEY to listOf("web", "button"),
      APP_TITLE_KEY to listOf("web", "title"),
    )

  data class DemoProjectScreenshotReference(
    val keyName: String,
    val positions: List<DemoProjectScreenshotReferencePosition>,
  )

  data class DemoProjectScreenshotReferencePosition(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
  )
}
