package io.tolgee.component.demoProject

import io.tolgee.model.Language

object DemoProjectData {
  val translations = mapOf(
    "en" to mapOf(
      "add-item-add-button" to "Add",
      "add-item-input-placeholder" to "New list item",
      "delete-item-button" to "Delete",
      "menu-item-translation-methods" to "Translation methods",
      "send-via-email" to "Send via e-mail",
      "share-button" to "Share",
      "app-title" to "What to pack",
    ),
    "de" to mapOf(
      "add-item-add-button" to "Einfügen",
      "add-item-input-placeholder" to "Neuer Eintrag",
      "delete-item-button" to "Löschen",
      "menu-item-translation-methods" to "Übersetzungsmethoden",
      "send-via-email" to "Per Email abschicken",
      "share-button" to "Teilen",
      "app-title" to "Was mitnehmen"
    ),
    "fr" to mapOf(
      "add-item-add-button" to "Ajouter",
      "add-item-input-placeholder" to "Nouvel élément de la liste",
      "delete-item-button" to "Supprimer",
      "menu-item-translation-methods" to "Méthodes de traduction",
      "send-via-email" to "Envoyer par courriel",
      "share-button" to "Partager",
      "app-title" to "Quoi emballer"
    ),
  )

  val screenshots = listOf(
    DemoProjectScreenshotReference(
      keyName = "add-item-add-button",
      positions = listOf(DemoProjectScreenshotReferencePosition(1601, 359, 168, 86))
    ),
    DemoProjectScreenshotReference(
      keyName = "add-item-input-placeholder",
      positions = listOf(DemoProjectScreenshotReferencePosition(617, 359, 951, 86))
    ),
    DemoProjectScreenshotReference(
      keyName = "delete-item-button",
      positions = listOf(
        DemoProjectScreenshotReferencePosition(1637, 568, 116, 35),
        DemoProjectScreenshotReferencePosition(1637, 637, 116, 35),
        DemoProjectScreenshotReferencePosition(1637, 707, 116, 35)
      )
    ),
    DemoProjectScreenshotReference(
      keyName = "menu-item-translation-methods",
      positions = listOf(DemoProjectScreenshotReferencePosition(564, 27, 290, 60))
    ),
    DemoProjectScreenshotReference(
      keyName = "send-via-email",
      positions = listOf(DemoProjectScreenshotReferencePosition(1439, 995, 331, 86))
    ),
    DemoProjectScreenshotReference(
      keyName = "share-button",
      positions = listOf(DemoProjectScreenshotReferencePosition(1202, 995, 204, 86))
    ),
    DemoProjectScreenshotReference(
      keyName = "app-title",
      positions = listOf(DemoProjectScreenshotReferencePosition(956, 181, 475, 91))
    ),
  )

  val inTranslatedState = mapOf("fr" to "add-item-add-button")

  val languages
    get() = listOf(
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

  data class DemoProjectScreenshotReference(
    val keyName: String,
    val positions: List<DemoProjectScreenshotReferencePosition>
  )

  data class DemoProjectScreenshotReferencePosition(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
  )
}
