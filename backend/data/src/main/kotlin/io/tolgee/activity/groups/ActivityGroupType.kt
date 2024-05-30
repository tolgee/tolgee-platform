package io.tolgee.activity.groups

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.RevisionType
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Tag
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.model.webhook.WebhookConfig

enum class ActivityGroupType(
  val sourceActivityTypes: List<ActivityType>,
  val allowedGroupEntityModificationDefinitions: List<GroupEntityModificationDefinition<*>>,
) {
  SET_TRANSLATION_STATE(
    listOf(ActivityType.SET_TRANSLATION_STATE, ActivityType.COMPLEX_EDIT),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Translation::state, Translation::outdated, Translation::mtProvider),
      ),
    ),
  ),

  REVIEW(
    listOf(ActivityType.SET_TRANSLATION_STATE, ActivityType.COMPLEX_EDIT),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Translation::state, Translation::outdated, Translation::mtProvider),
        allowedValues = mapOf(Translation::state to TranslationState.REVIEWED),
      ),
    ),
  ),

  SET_TRANSLATIONS(
    listOf(ActivityType.SET_TRANSLATIONS, ActivityType.COMPLEX_EDIT),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.ADD, RevisionType.DEL, RevisionType.MOD),
      ),
    ),
  ),

  DISMISS_AUTO_TRANSLATED_STATE(
    listOf(ActivityType.DISMISS_AUTO_TRANSLATED_STATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Translation::auto, Translation::mtProvider),
        allowedValues = mapOf(Translation::mtProvider to null, Translation::auto to false),
      ),
    ),
  ),

  SET_OUTDATED_FLAG(
    listOf(ActivityType.SET_OUTDATED_FLAG),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Translation::outdated),
      ),
    ),
  ),

  TRANSLATION_COMMENT_ADD(
    listOf(ActivityType.TRANSLATION_COMMENT_ADD),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = TranslationComment::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
    ),
  ),

  TRANSLATION_COMMENT_DELETE(
    listOf(ActivityType.TRANSLATION_COMMENT_DELETE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = TranslationComment::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
    ),
  ),

  TRANSLATION_COMMENT_EDIT(
    listOf(ActivityType.TRANSLATION_COMMENT_EDIT),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = TranslationComment::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(TranslationComment::text),
      ),
    ),
  ),

  TRANSLATION_COMMENT_SET_STATE(
    listOf(ActivityType.TRANSLATION_COMMENT_SET_STATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = TranslationComment::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(TranslationComment::state),
      ),
    ),
  ),

  SCREENSHOT_DELETE(
    listOf(ActivityType.SCREENSHOT_DELETE, ActivityType.COMPLEX_EDIT),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Screenshot::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
      GroupEntityModificationDefinition(
        entityClass = KeyScreenshotReference::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
    ),
  ),

  SCREENSHOT_ADD(
    listOf(ActivityType.SCREENSHOT_ADD, ActivityType.COMPLEX_EDIT),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Screenshot::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
      GroupEntityModificationDefinition(
        entityClass = KeyScreenshotReference::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
    ),
  ),

  KEY_TAGS_EDIT(
    listOf(
      ActivityType.KEY_TAGS_EDIT,
      ActivityType.COMPLEX_EDIT,
      ActivityType.BATCH_TAG_KEYS,
      ActivityType.BATCH_UNTAG_KEYS,
    ),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = KeyMeta::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(KeyMeta::tags),
      ),
      GroupEntityModificationDefinition(
        entityClass = Tag::class,
        revisionTypes = listOf(RevisionType.ADD, RevisionType.DEL),
      ),
    ),
  ),

  KEY_NAME_EDIT(
    listOf(ActivityType.KEY_NAME_EDIT, ActivityType.COMPLEX_EDIT),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Key::name, Key::namespace),
      ),
      GroupEntityModificationDefinition(
        entityClass = Namespace::class,
        revisionTypes = listOf(RevisionType.ADD, RevisionType.DEL),
      ),
    ),
  ),

  KEY_DELETE(
    listOf(ActivityType.KEY_DELETE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
    ),
  ),

  KEY_CREATE(
    listOf(ActivityType.CREATE_KEY),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
      GroupEntityModificationDefinition(
        entityClass = KeyMeta::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
    ),
  ),

  IMPORT(
    listOf(ActivityType.IMPORT),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
      GroupEntityModificationDefinition(
        entityClass = KeyMeta::class,
        revisionTypes = listOf(RevisionType.ADD, RevisionType.MOD),
        modificationProps = listOf(KeyMeta::custom, KeyMeta::description),
      ),
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
      GroupEntityModificationDefinition(
        entityClass = Namespace::class,
        revisionTypes = listOf(RevisionType.ADD, RevisionType.DEL),
      ),
    ),
  ),

  CREATE_LANGUAGE(
    listOf(ActivityType.CREATE_LANGUAGE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Language::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
    ),
  ),

  EDIT_LANGUAGE(
    listOf(ActivityType.EDIT_LANGUAGE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Language::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Language::name, Language::tag, Language::originalName, Language::flagEmoji),
      ),
    ),
  ),

  DELETE_LANGUAGE(
    listOf(ActivityType.DELETE_LANGUAGE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Language::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
    ),
  ),

  CREATE_PROJECT(
    listOf(ActivityType.CREATE_PROJECT),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Project::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
    ),
  ),

  EDIT_PROJECT(
    listOf(ActivityType.EDIT_PROJECT),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Project::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps =
          listOf(
            Project::name,
            Project::description,
            Project::baseLanguage,
            Project::defaultNamespace,
            Project::avatarHash,
          ),
      ),
    ),
  ),

  NAMESPACE_EDIT(
    listOf(ActivityType.NAMESPACE_EDIT, ActivityType.BATCH_SET_KEYS_NAMESPACE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Namespace::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Namespace::name),
      ),
    ),
  ),

  BATCH_PRE_TRANSLATE_BY_TM(
    listOf(ActivityType.BATCH_PRE_TRANSLATE_BY_TM),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(Translation::state, Translation::text, Translation::outdated, Translation::auto),
      ),
    ),
  ),

  BATCH_MACHINE_TRANSLATE(
    listOf(ActivityType.BATCH_MACHINE_TRANSLATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(Translation::state, Translation::text, Translation::outdated, Translation::auto),
      ),
    ),
  ),

  AUTO_TRANSLATE(
    listOf(ActivityType.AUTO_TRANSLATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(Translation::state, Translation::text, Translation::outdated, Translation::auto),
      ),
    ),
  ),

  BATCH_CLEAR_TRANSLATIONS(
    listOf(ActivityType.BATCH_CLEAR_TRANSLATIONS),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Translation::state, Translation::text, Translation::outdated, Translation::auto),
        allowedValues =
          mapOf(
            Translation::text to null,
            Translation::state to null,
            Translation::outdated to false,
            Translation::auto to false,
          ),
      ),
    ),
  ),

  BATCH_COPY_TRANSLATIONS(
    listOf(ActivityType.BATCH_COPY_TRANSLATIONS),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(Translation::state, Translation::text, Translation::outdated, Translation::auto),
      ),
    ),
  ),

  BATCH_SET_TRANSLATION_STATE(
    listOf(ActivityType.BATCH_SET_TRANSLATION_STATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Translation::state, Translation::outdated, Translation::auto),
      ),
    ),
  ),

  CONTENT_DELIVERY_CONFIG_CREATE(
    listOf(ActivityType.CONTENT_DELIVERY_CONFIG_CREATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = ContentDeliveryConfig::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
    ),
  ),

  CONTENT_DELIVERY_CONFIG_UPDATE(
    listOf(ActivityType.CONTENT_DELIVERY_CONFIG_UPDATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = ContentDeliveryConfig::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
    ),
  ),

  CONTENT_DELIVERY_CONFIG_DELETE(
    listOf(ActivityType.CONTENT_DELIVERY_CONFIG_UPDATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = ContentDeliveryConfig::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
    ),
  ),

  CONTENT_STORAGE_CREATE(
    listOf(ActivityType.CONTENT_STORAGE_CREATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = ContentStorage::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
    ),
  ),

  CONTENT_STORAGE_UPDATE(
    listOf(ActivityType.CONTENT_STORAGE_UPDATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = ContentStorage::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
    ),
  ),

  CONTENT_STORAGE_DELETE(
    listOf(ActivityType.CONTENT_STORAGE_DELETE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = ContentStorage::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
    ),
  ),

  WEBHOOK_CONFIG_CREATE(
    listOf(ActivityType.WEBHOOK_CONFIG_CREATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = WebhookConfig::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
    ),
  ),

  WEBHOOK_CONFIG_UPDATE(
    listOf(ActivityType.WEBHOOK_CONFIG_CREATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = WebhookConfig::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
    ),
  ),

  WEBHOOK_CONFIG_DELETE(
    listOf(ActivityType.WEBHOOK_CONFIG_CREATE),
    listOf(
      GroupEntityModificationDefinition(
        entityClass = WebhookConfig::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
    ),
  ),
}
