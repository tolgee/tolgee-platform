package io.tolgee.activity.groups

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.RevisionType
import io.tolgee.activity.groups.matchers.ActivityGroupValueMatcher.Companion.eq
import io.tolgee.activity.groups.matchers.ActivityGroupValueMatcher.Companion.modification
import io.tolgee.activity.groups.matchers.ActivityGroupValueMatcher.Companion.notNull
import io.tolgee.activity.groups.matchers.modifiedEntity.DefaultMatcher
import io.tolgee.activity.groups.matchers.modifiedEntity.ModifiedEntityMatcher
import io.tolgee.activity.groups.viewProviders.createProject.CreateProjectGroupModelProvider
import io.tolgee.activity.groups.viewProviders.keyCreate.CreateKeyGroupModelProvider
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
import kotlin.reflect.KClass

enum class ActivityGroupType(
  val sourceActivityTypes: List<ActivityType>,
  val modelProviderFactoryClass: KClass<out GroupModelProvider<*, *>>? = null,
  val matcher: ModifiedEntityMatcher? = null,
) {
  SET_TRANSLATION_STATE(
    listOf(ActivityType.SET_TRANSLATION_STATE, ActivityType.COMPLEX_EDIT),
    matcher =
      DefaultMatcher(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(Translation::state, Translation::outdated, Translation::mtProvider),
        deniedValues =
          mapOf(
            Translation::state to TranslationState.REVIEWED,
            Translation::text to modification(eq(null) to notNull()),
          ),
      ),
  ),
  REVIEW(
    listOf(ActivityType.SET_TRANSLATION_STATE, ActivityType.COMPLEX_EDIT),
    matcher =
      DefaultMatcher(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(Translation::state, Translation::outdated, Translation::mtProvider),
        allowedValues = mapOf(Translation::state to TranslationState.REVIEWED),
      ),
  ),

  SET_TRANSLATIONS(
    listOf(ActivityType.SET_TRANSLATIONS, ActivityType.COMPLEX_EDIT),
    matcher =
      DefaultMatcher(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.ADD, RevisionType.DEL, RevisionType.MOD),
        modificationProps = listOf(Translation::text),
      ),
  ),

  DISMISS_AUTO_TRANSLATED_STATE(
    listOf(ActivityType.DISMISS_AUTO_TRANSLATED_STATE),
    matcher =
      DefaultMatcher(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Translation::auto, Translation::mtProvider),
        allowedValues = mapOf(Translation::mtProvider to null, Translation::auto to false),
      ),
  ),

  SET_OUTDATED_FLAG(
    listOf(ActivityType.SET_OUTDATED_FLAG),
    matcher =
      DefaultMatcher(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Translation::outdated),
      ),
  ),

  ADD_TRANSLATION_COMMENT(
    listOf(ActivityType.TRANSLATION_COMMENT_ADD),
    matcher =
      DefaultMatcher(
        entityClass = TranslationComment::class,
        revisionTypes = listOf(RevisionType.ADD),
      ).or(
        DefaultMatcher(
          entityClass = Translation::class,
          revisionTypes = listOf(RevisionType.ADD),
        ),
      ),
  ),

  DELETE_TRANSLATION_COMMENT(
    listOf(ActivityType.TRANSLATION_COMMENT_DELETE),
    matcher =
      DefaultMatcher(
        entityClass = TranslationComment::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  EDIT_TRANSLATION_COMMENT(
    listOf(ActivityType.TRANSLATION_COMMENT_EDIT),
    matcher =
      DefaultMatcher(
        entityClass = TranslationComment::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(TranslationComment::text),
      ),
  ),

  SET_TRANSLATION_COMMENT_STATE(
    listOf(ActivityType.TRANSLATION_COMMENT_SET_STATE),
    matcher =
      DefaultMatcher(
        entityClass = TranslationComment::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(TranslationComment::state),
      ),
  ),

  DELETE_SCREENSHOT(
    listOf(ActivityType.SCREENSHOT_DELETE, ActivityType.COMPLEX_EDIT),
    matcher =
      DefaultMatcher(
        entityClass = Screenshot::class,
        revisionTypes = listOf(RevisionType.DEL),
      ).or(
        DefaultMatcher(
          entityClass = KeyScreenshotReference::class,
          revisionTypes = listOf(RevisionType.DEL),
        ),
      ),
  ),

  ADD_SCREENSHOT(
    listOf(ActivityType.SCREENSHOT_ADD, ActivityType.COMPLEX_EDIT),
    matcher =
      DefaultMatcher(
        entityClass = Screenshot::class,
        revisionTypes = listOf(RevisionType.ADD),
      ).or(
        DefaultMatcher(
          entityClass = KeyScreenshotReference::class,
          revisionTypes = listOf(RevisionType.ADD),
        ),
      ),
  ),

  EDIT_KEY_TAGS(
    listOf(
      ActivityType.KEY_TAGS_EDIT,
      ActivityType.COMPLEX_EDIT,
      ActivityType.BATCH_TAG_KEYS,
      ActivityType.BATCH_UNTAG_KEYS,
    ),
    matcher =
      DefaultMatcher(
        entityClass = KeyMeta::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(KeyMeta::tags),
      ).or(
        DefaultMatcher(
          entityClass = Tag::class,
          revisionTypes = listOf(RevisionType.ADD, RevisionType.DEL),
        ),
      ),
  ),

  EDIT_KEY_NAME(
    listOf(ActivityType.KEY_NAME_EDIT, ActivityType.COMPLEX_EDIT),
    matcher =
      DefaultMatcher(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Key::name, Key::namespace),
      ).or(
        DefaultMatcher(
          entityClass = Namespace::class,
          revisionTypes = listOf(RevisionType.ADD, RevisionType.DEL),
        ),
      ),
  ),

  DELETE_KEY(
    listOf(ActivityType.KEY_DELETE),
    matcher =
      DefaultMatcher(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  CREATE_KEY(
    listOf(ActivityType.CREATE_KEY),
    matcher =
      DefaultMatcher(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.ADD),
      ).or(
        DefaultMatcher(
          entityClass = KeyMeta::class,
          revisionTypes = listOf(RevisionType.ADD),
        ),
      ).or(
        DefaultMatcher(
          Translation::class,
          listOf(RevisionType.ADD),
        ),
      ),
    modelProviderFactoryClass = CreateKeyGroupModelProvider::class,
  ),

  IMPORT(
    listOf(ActivityType.IMPORT),
  ),

  CREATE_LANGUAGE(
    listOf(ActivityType.CREATE_LANGUAGE),
    matcher =
      DefaultMatcher(
        entityClass = Language::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  EDIT_LANGUAGE(
    listOf(ActivityType.EDIT_LANGUAGE),
    matcher =
      DefaultMatcher(
        entityClass = Language::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Language::name, Language::tag, Language::originalName, Language::flagEmoji),
      ),
  ),

  DELETE_LANGUAGE(
    listOf(ActivityType.DELETE_LANGUAGE),
    matcher =
      DefaultMatcher(
        entityClass = Language::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  CREATE_PROJECT(
    listOf(ActivityType.CREATE_PROJECT),
    matcher =
      DefaultMatcher(
        entityClass = Project::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
    modelProviderFactoryClass = CreateProjectGroupModelProvider::class,
  ),

  EDIT_PROJECT(
    listOf(ActivityType.EDIT_PROJECT),
    matcher =
      DefaultMatcher(
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

  NAMESPACE_EDIT(
    listOf(ActivityType.NAMESPACE_EDIT, ActivityType.BATCH_SET_KEYS_NAMESPACE),
    matcher =
      DefaultMatcher(
        entityClass = Namespace::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Namespace::name),
      ),
  ),

  BATCH_PRE_TRANSLATE_BY_TM(
    listOf(ActivityType.BATCH_PRE_TRANSLATE_BY_TM),
    matcher =
      DefaultMatcher(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(Translation::state, Translation::text, Translation::outdated, Translation::auto),
      ),
  ),

  BATCH_MACHINE_TRANSLATE(
    listOf(ActivityType.BATCH_MACHINE_TRANSLATE),
    matcher =
      DefaultMatcher(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(Translation::state, Translation::text, Translation::outdated, Translation::auto),
      ),
  ),

  AUTO_TRANSLATE(
    listOf(ActivityType.AUTO_TRANSLATE),
    matcher =
      DefaultMatcher(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(Translation::state, Translation::text, Translation::outdated, Translation::auto),
      ),
  ),

  BATCH_CLEAR_TRANSLATIONS(
    listOf(ActivityType.BATCH_CLEAR_TRANSLATIONS),
    matcher =
      DefaultMatcher(
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

  BATCH_COPY_TRANSLATIONS(
    listOf(ActivityType.BATCH_COPY_TRANSLATIONS),
    matcher =
      DefaultMatcher(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(Translation::state, Translation::text, Translation::outdated, Translation::auto),
      ),
  ),

  BATCH_SET_TRANSLATION_STATE(
    listOf(ActivityType.BATCH_SET_TRANSLATION_STATE),
    matcher =
      DefaultMatcher(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Translation::state, Translation::outdated, Translation::auto),
      ),
  ),

  CONTENT_DELIVERY_CONFIG_CREATE(
    listOf(ActivityType.CONTENT_DELIVERY_CONFIG_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = ContentDeliveryConfig::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  CONTENT_DELIVERY_CONFIG_UPDATE(
    listOf(ActivityType.CONTENT_DELIVERY_CONFIG_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = ContentDeliveryConfig::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
  ),

  CONTENT_DELIVERY_CONFIG_DELETE(
    listOf(ActivityType.CONTENT_DELIVERY_CONFIG_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = ContentDeliveryConfig::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  CONTENT_STORAGE_CREATE(
    listOf(ActivityType.CONTENT_STORAGE_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = ContentStorage::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  CONTENT_STORAGE_UPDATE(
    listOf(ActivityType.CONTENT_STORAGE_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = ContentStorage::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
  ),

  CONTENT_STORAGE_DELETE(
    listOf(ActivityType.CONTENT_STORAGE_DELETE),
    matcher =
      DefaultMatcher(
        entityClass = ContentStorage::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  WEBHOOK_CONFIG_CREATE(
    listOf(ActivityType.WEBHOOK_CONFIG_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = WebhookConfig::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  WEBHOOK_CONFIG_UPDATE(
    listOf(ActivityType.WEBHOOK_CONFIG_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = WebhookConfig::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
  ),

  WEBHOOK_CONFIG_DELETE(
    listOf(ActivityType.WEBHOOK_CONFIG_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = WebhookConfig::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  ;

  fun getProvidingModelTypes(): Pair<KClass<*>?, KClass<*>?>? {
    val arguments =
      modelProviderFactoryClass
        ?.supertypes
        ?.firstOrNull()
        ?.arguments ?: return null

    val groupType =
      arguments.firstOrNull()
        ?.type
        ?.classifier as? KClass<*>

    val itemType =
      arguments[1]
        .type
        ?.classifier as? KClass<*>

    return groupType to itemType
  }
}
