package io.tolgee.model.views

interface ImportTranslationView {
    val id: Long
    val text: String
    val keyName: String
    val keyId: Long
    val collisionId: Long?
    val collisionText: String?
    val override: Boolean
}
