package io.tolgee.hateoas.notification

import io.tolgee.hateoas.translations.comments.MentionedUserModel
import java.io.Serializable

data class LinkedCommentModel(
  val id: Long,
  val text: String,
  val mentions: List<MentionedUserModel>,
  val keyId: Long,
  val keyName: String,
  val keyNamespace: String?,
  val languageId: Long,
  val languageTag: String,
  val translationId: Long,
) : Serializable
