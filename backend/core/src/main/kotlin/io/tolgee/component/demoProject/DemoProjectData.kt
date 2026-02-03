package io.tolgee.component.demoProject

import io.tolgee.model.Language
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val ADD_ITEM_ADD_BUTTON_KEY = "add-item-add-button"
private const val ADD_ITEM_INPUT_PLACEHOLDER_KEY = "add-item-input-placeholder"
private const val DELETE_ITEM_BUTTON_KEY = "delete-item-button"
private const val MENU_ITEM_TRANSLATION_METHODS_KEY = "menu-item-translation-methods"
private const val SEND_VIA_EMAIL_KEY = "send-via-email"
private const val SHARE_BUTTON_KEY = "share-button"
private const val APP_TITLE_KEY = "app-title"
private const val ITEMS_COUNT_KEY = "items-count"
private const val WELCOME_MESSAGE = "welcome-message"
private const val TERMS_CONDITIONS_AGREEMENT_KEY = "terms-conditions-agreement"

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
          ITEMS_COUNT_KEY to "{count, plural, one {Showing # item} other {Showing # items}}",
          WELCOME_MESSAGE to "Hello {name}!",
          TERMS_CONDITIONS_AGREEMENT_KEY to
            "I agree with the <a>Terms and Conditions</a>",
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
          ITEMS_COUNT_KEY to "{count, plural, one {Zeige # Eintrag} other {Zeige # Einträge}}",
          WELCOME_MESSAGE to "Hallo {name}!",
          TERMS_CONDITIONS_AGREEMENT_KEY to
            "Ich stimme den <a>Allgemeinen Geschäftsbedingungen</a> zu",
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
          ITEMS_COUNT_KEY to "{count, plural, one {Affiche # élément} other {Affiche # éléments}}",
          WELCOME_MESSAGE to "Bonjour {name}!",
          TERMS_CONDITIONS_AGREEMENT_KEY to
            "J'accepte les <a>Conditions générales</a>",
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

  val pluralArgNames = mapOf(ITEMS_COUNT_KEY to "count")

  val tags =
    mapOf(
      ADD_ITEM_ADD_BUTTON_KEY to listOf("web", "button", "draft"),
      SHARE_BUTTON_KEY to listOf("web", "button"),
      DELETE_ITEM_BUTTON_KEY to listOf("web", "button"),
      MENU_ITEM_TRANSLATION_METHODS_KEY to listOf("web", "menu"),
      SEND_VIA_EMAIL_KEY to listOf("web", "button"),
      APP_TITLE_KEY to listOf("web", "title"),
    )

  val descriptions =
    mapOf(
      ADD_ITEM_ADD_BUTTON_KEY to "Text for the 'Add' button in the 'Add Item' context",
      ADD_ITEM_INPUT_PLACEHOLDER_KEY to "Placeholder text for the input field when adding a new list item",
      DELETE_ITEM_BUTTON_KEY to "Text for the 'Delete' button in the item context",
      MENU_ITEM_TRANSLATION_METHODS_KEY to "Text for the menu item linking to translation methods",
      SEND_VIA_EMAIL_KEY to "Text for the option to send the items to pack via email",
      SHARE_BUTTON_KEY to "Text for the share (on social media, etc) button",
      APP_TITLE_KEY to "Title of the application",
      ITEMS_COUNT_KEY to "Text displaying the number of items",
      WELCOME_MESSAGE to "Welcome message including a placeholder for the user's name",
      TERMS_CONDITIONS_AGREEMENT_KEY to "Statement for accepting the Terms and Conditions that includes a hyperlink",
    )

  val alice =
    DemoUser(
      name = "Alice Smith",
      username = "alice-demo@demo.tolgee.io",
      avatarFileName = "alice.png",
    )

  val bob =
    DemoUser(
      name = "Bob Johnson",
      username = "bob-demo@demo.tolgee.io",
      avatarFileName = "bob.png",
    )

  val charlie =
    DemoUser(
      name = "Charlie Brown",
      username = "charlie-demo@demo.tolgee.io",
      avatarFileName = "charlie.png",
    )

  val demoUsers: List<DemoUser> =
    listOf(
      alice,
      bob,
      charlie,
    )

  val comments =
    listOf(
      Comment(APP_TITLE_KEY, "de", "This is wrong!", alice, 10.toDuration(DurationUnit.HOURS)),
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

  data class DemoUser(
    val name: String,
    /**
     * The e-mail
     */
    val username: String,
    val avatarFileName: String,
  )

  data class Comment(
    val key: String,
    val language: String,
    val text: String,
    val author: DemoUser,
    val createdBefore: Duration,
  )
}
