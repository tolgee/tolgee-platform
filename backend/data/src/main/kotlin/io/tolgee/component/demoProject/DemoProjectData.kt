package io.tolgee.component.demoProject

import io.tolgee.model.Language

object DemoProjectData {
  val translations = mapOf(
    "en" to mapOf(
      "add-item-add-button" to "Add",
      "add-item-input-placeholder" to "New list item",
      "delete-item-button" to "Delete",
      "menu-item-translation-methods" to "Translation methods",
      "on-the-road-subtitle" to "What to pack for the trip",
      "on-the-road-title" to "On the road",
      "send-via-email" to "Send via e-mail",
      "share-button" to "Share",
    ),
    "de" to mapOf(
      "add-item-add-button" to "Einfügen",
      "add-item-input-placeholder" to "Neuer Eintrag",
      "delete-item-button" to "Löschen",
      "menu-item-translation-methods" to "Übersetzungsmethoden",
      "on-the-road-subtitle" to "Was zum Ausflug einzupacken",
      "on-the-road-title" to "Auf dem Weg",
      "send-via-email" to "Per Email abschicken",
      "share-button" to "Teilen",
    ),
    "fr" to mapOf(
      "add-item-add-button" to "Ajouter",
      "add-item-input-placeholder" to "Nouvel élément de la liste",
      "delete-item-button" to "Supprimer",
      "menu-item-translation-methods" to "Méthodes de traduction",
      "on-the-road-subtitle" to "Comment faire sa valise pour la randonnée",
      "on-the-road-title" to "Sur la route",
      "send-via-email" to "Envoyer par courriel",
      "share-button" to "Partager",
    ),
    "ar-SA" to mapOf(
      "add-item-add-button" to "إضافة",
      "add-item-input-placeholder" to "عنصر جديد في القائمة",
      "delete-item-button" to "حذف",
      "menu-item-translation-methods" to "طرق الترجمة",
      "on-the-road-subtitle" to "كيفية تعبئة حقيبتك للرحلة",
      "on-the-road-title" to "في الطريق",
      "send-via-email" to "إرسال عبر البريد الإلكتروني",
      "share-button" to "مشاركة",
    )
  )

  val inTranslatedState = mapOf("fr" to "add-item-add-button", "ar-SA" to "add-item-add-button")

  val languages = listOf(
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
    Language().apply {
      name = "Arabic (Saudi Arabia)"
      tag = "ar-SA"
      originalName = "العربية (السعودية)"
      flagEmoji = "🇸🇦"
    }
  )
}
