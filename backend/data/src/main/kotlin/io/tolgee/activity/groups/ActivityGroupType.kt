package io.tolgee.activity.groups

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.RevisionType
import io.tolgee.activity.groups.matchers.ActivityGroupValueMatcher.Companion.eq
import io.tolgee.activity.groups.matchers.ActivityGroupValueMatcher.Companion.modification
import io.tolgee.activity.groups.matchers.ActivityGroupValueMatcher.Companion.notNull
import io.tolgee.activity.groups.matchers.modifiedEntity.DefaultMatcher
import io.tolgee.activity.groups.matchers.modifiedEntity.MatchingStringProvider
import io.tolgee.activity.groups.matchers.modifiedEntity.ModifiedEntityMatcher
import io.tolgee.activity.groups.matchers.modifiedEntity.SetTranslationMatchingStringProvider
import io.tolgee.activity.groups.matchers.modifiedEntity.TranslationMatcher
import io.tolgee.activity.groups.viewProviders.createKey.CreateKeyGroupModelProvider
import io.tolgee.activity.groups.viewProviders.createProject.CreateProjectGroupModelProvider
import io.tolgee.activity.groups.viewProviders.setTranslations.SetTranslationsGroupModelProvider
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Prompt
import io.tolgee.model.Screenshot
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.branching.Branch
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Tag
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskKey
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.webhook.WebhookConfig
import kotlin.reflect.KClass

enum class ActivityGroupType(
  val sourceActivityTypes: List<ActivityType>,
  val modelProviderFactoryClass: KClass<out GroupModelProvider<*, *>>? = null,
  val matcher: ModifiedEntityMatcher? = null,
  val matchingStringProvider: MatchingStringProvider? = null,
  /**
   * Even if we are working with same revisions order can be unnatural in some cases.
   * e.g. when user creates a key and then sets translations for it. It results in multiple groups.
   * We need to force the order, so key creation is always before translation set
   */
  val orderAfter: ActivityGroupType? = null,
) {
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
        TranslationMatcher(TranslationMatcher.Type.BASE),
      ),
    modelProviderFactoryClass = CreateKeyGroupModelProvider::class,
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

  SET_BASE_TRANSLATION(
    listOf(ActivityType.SET_TRANSLATIONS, ActivityType.COMPLEX_EDIT),
    matcher =
      TranslationMatcher(TranslationMatcher.Type.BASE),
  ),

  SET_TRANSLATIONS(
    listOf(ActivityType.SET_TRANSLATIONS, ActivityType.COMPLEX_EDIT, ActivityType.CREATE_KEY),
    matcher =
      TranslationMatcher(TranslationMatcher.Type.NON_BASE),
    matchingStringProvider = SetTranslationMatchingStringProvider(),
    modelProviderFactoryClass = SetTranslationsGroupModelProvider::class,
    orderAfter = CREATE_KEY,
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
      ActivityType.COMPLEX_TAG_OPERATION,
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
        // language deletion is a soft delete followed by an async hard delete
        revisionTypes = listOf(RevisionType.DEL, RevisionType.MOD),
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
    listOf(ActivityType.CONTENT_DELIVERY_CONFIG_DELETE),
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
    listOf(ActivityType.WEBHOOK_CONFIG_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = WebhookConfig::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
  ),

  WEBHOOK_CONFIG_DELETE(
    listOf(ActivityType.WEBHOOK_CONFIG_DELETE),
    matcher =
      DefaultMatcher(
        entityClass = WebhookConfig::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  EDIT_KEY_CHARACTER_LIMIT(
    listOf(ActivityType.KEY_CHARACTER_LIMIT_EDIT),
    matcher =
      DefaultMatcher(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Key::maxCharLimit),
      ),
  ),

  SOFT_DELETE_KEY(
    listOf(ActivityType.KEY_SOFT_DELETE),
    matcher =
      DefaultMatcher(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Key::deletedAt),
        allowedValues = mapOf(Key::deletedAt to notNull()),
      ),
  ),

  RESTORE_KEY(
    listOf(ActivityType.KEY_RESTORE, ActivityType.BATCH_KEY_RESTORE),
    matcher =
      DefaultMatcher(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Key::deletedAt),
        allowedValues = mapOf(Key::deletedAt to null),
      ),
  ),

  HARD_DELETE_KEY(
    listOf(ActivityType.KEY_HARD_DELETE, ActivityType.BATCH_KEY_HARD_DELETE),
    matcher =
      DefaultMatcher(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  SET_TRANSLATION_LABELS(
    listOf(
      ActivityType.TRANSLATION_LABEL_ASSIGN,
      ActivityType.BATCH_ASSIGN_TRANSLATION_LABEL,
      ActivityType.BATCH_UNASSIGN_TRANSLATION_LABEL,
      ActivityType.TRANSLATION_LABELS_EDIT,
    ),
    matcher =
      DefaultMatcher(
        entityClass = Translation::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Translation::labels),
      ),
  ),

  CREATE_TRANSLATION_LABEL(
    listOf(ActivityType.TRANSLATION_LABEL_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = Label::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  EDIT_TRANSLATION_LABEL(
    listOf(ActivityType.TRANSLATION_LABEL_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = Label::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
  ),

  DELETE_TRANSLATION_LABEL(
    listOf(ActivityType.TRANSLATION_LABEL_DELETE),
    matcher =
      DefaultMatcher(
        entityClass = Label::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  CREATE_TASK(
    listOf(ActivityType.TASK_CREATE, ActivityType.TASKS_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = Task::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  EDIT_TASK(
    listOf(ActivityType.TASK_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = Task::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Task::name, Task::description, Task::type, Task::dueDate, Task::assignees),
      ),
  ),

  FINISH_TASK(
    listOf(ActivityType.TASK_FINISH),
    matcher =
      DefaultMatcher(
        entityClass = Task::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Task::state, Task::closedAt),
      ),
  ),

  CLOSE_TASK(
    listOf(ActivityType.TASK_CLOSE),
    matcher =
      DefaultMatcher(
        entityClass = Task::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Task::state, Task::closedAt),
      ),
  ),

  REOPEN_TASK(
    listOf(ActivityType.TASK_REOPEN),
    matcher =
      DefaultMatcher(
        entityClass = Task::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Task::state, Task::closedAt),
      ),
  ),

  UPDATE_TASK_KEYS(
    listOf(ActivityType.TASK_KEYS_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = TaskKey::class,
        revisionTypes = listOf(RevisionType.ADD, RevisionType.DEL),
      ),
  ),

  CREATE_GLOSSARY(
    listOf(ActivityType.GLOSSARY_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = Glossary::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  EDIT_GLOSSARY(
    listOf(ActivityType.GLOSSARY_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = Glossary::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
  ),

  DELETE_GLOSSARY(
    listOf(ActivityType.GLOSSARY_DELETE),
    matcher =
      DefaultMatcher(
        entityClass = Glossary::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  IMPORT_GLOSSARY(
    listOf(ActivityType.GLOSSARY_IMPORT),
    matcher =
      DefaultMatcher(
        entityClass = GlossaryTerm::class,
        revisionTypes = listOf(RevisionType.ADD, RevisionType.MOD),
      ).or(
        DefaultMatcher(
          entityClass = GlossaryTermTranslation::class,
          revisionTypes = listOf(RevisionType.ADD, RevisionType.MOD),
        ),
      ),
  ),

  CREATE_GLOSSARY_TERM(
    listOf(ActivityType.GLOSSARY_TERM_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = GlossaryTerm::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  EDIT_GLOSSARY_TERM(
    listOf(ActivityType.GLOSSARY_TERM_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = GlossaryTerm::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
  ),

  DELETE_GLOSSARY_TERM(
    listOf(ActivityType.GLOSSARY_TERM_DELETE),
    matcher =
      DefaultMatcher(
        entityClass = GlossaryTerm::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  SET_GLOSSARY_TERM_TRANSLATION(
    listOf(ActivityType.GLOSSARY_TERM_TRANSLATION_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = GlossaryTermTranslation::class,
        revisionTypes = listOf(RevisionType.ADD, RevisionType.MOD),
        modificationProps = listOf(GlossaryTermTranslation::text),
      ),
  ),

  CREATE_TRANSLATION_MEMORY(
    listOf(ActivityType.TRANSLATION_MEMORY_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = TranslationMemory::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  EDIT_TRANSLATION_MEMORY(
    listOf(ActivityType.TRANSLATION_MEMORY_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = TranslationMemory::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
  ),

  DELETE_TRANSLATION_MEMORY(
    listOf(ActivityType.TRANSLATION_MEMORY_DELETE),
    matcher =
      DefaultMatcher(
        entityClass = TranslationMemory::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  CREATE_SUGGESTION(
    listOf(ActivityType.CREATE_SUGGESTION),
    matcher =
      DefaultMatcher(
        entityClass = TranslationSuggestion::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  DELETE_SUGGESTION(
    listOf(ActivityType.DELETE_SUGGESTION),
    matcher =
      DefaultMatcher(
        entityClass = TranslationSuggestion::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  DECLINE_SUGGESTION(
    listOf(ActivityType.DECLINE_SUGGESTION),
    matcher =
      DefaultMatcher(
        entityClass = TranslationSuggestion::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(TranslationSuggestion::state),
        allowedValues = mapOf(TranslationSuggestion::state to TranslationSuggestionState.DECLINED),
      ),
  ),

  ACCEPT_SUGGESTION(
    listOf(ActivityType.ACCEPT_SUGGESTION),
    matcher =
      DefaultMatcher(
        entityClass = TranslationSuggestion::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(TranslationSuggestion::state),
        allowedValues = mapOf(TranslationSuggestion::state to TranslationSuggestionState.ACCEPTED),
      ),
  ),

  SET_SUGGESTION_ACTIVE(
    listOf(ActivityType.SUGGESTION_SET_ACTIVE, ActivityType.REVERSE_SUGGESTION),
    matcher =
      DefaultMatcher(
        entityClass = TranslationSuggestion::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(TranslationSuggestion::state),
        allowedValues = mapOf(TranslationSuggestion::state to TranslationSuggestionState.ACTIVE),
      ),
  ),

  CREATE_AI_PROMPT(
    listOf(ActivityType.AI_PROMPT_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = Prompt::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  EDIT_AI_PROMPT(
    listOf(ActivityType.AI_PROMPT_UPDATE),
    matcher =
      DefaultMatcher(
        entityClass = Prompt::class,
        revisionTypes = listOf(RevisionType.MOD),
      ),
  ),

  DELETE_AI_PROMPT(
    listOf(ActivityType.AI_PROMPT_DELETE),
    matcher =
      DefaultMatcher(
        entityClass = Prompt::class,
        revisionTypes = listOf(RevisionType.DEL),
      ),
  ),

  CREATE_BRANCH(
    listOf(ActivityType.BRANCH_CREATE),
    matcher =
      DefaultMatcher(
        entityClass = Branch::class,
        revisionTypes = listOf(RevisionType.ADD),
      ),
  ),

  RENAME_BRANCH(
    listOf(ActivityType.BRANCH_RENAME),
    matcher =
      DefaultMatcher(
        entityClass = Branch::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Branch::name),
      ),
  ),

  CHANGE_BRANCH_PROTECTION(
    listOf(ActivityType.BRANCH_PROTECTION_CHANGE),
    matcher =
      DefaultMatcher(
        entityClass = Branch::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(Branch::isProtected),
      ),
  ),

  MERGE_BRANCH(
    listOf(ActivityType.BRANCH_MERGE),
    matcher =
      DefaultMatcher(
        entityClass = Key::class,
        revisionTypes = listOf(RevisionType.ADD, RevisionType.MOD, RevisionType.DEL),
      ).or(
        DefaultMatcher(
          entityClass = Translation::class,
          revisionTypes = listOf(RevisionType.ADD, RevisionType.MOD, RevisionType.DEL),
        ),
      ),
  ),

  IGNORE_QA_ISSUE(
    listOf(ActivityType.QA_ISSUE_IGNORE),
    matcher =
      DefaultMatcher(
        entityClass = TranslationQaIssue::class,
        revisionTypes = listOf(RevisionType.MOD, RevisionType.ADD),
        modificationProps = listOf(TranslationQaIssue::state),
        allowedValues = mapOf(TranslationQaIssue::state to QaIssueState.IGNORED),
      ),
  ),

  UNIGNORE_QA_ISSUE(
    listOf(ActivityType.QA_ISSUE_UNIGNORE),
    matcher =
      DefaultMatcher(
        entityClass = TranslationQaIssue::class,
        revisionTypes = listOf(RevisionType.MOD),
        modificationProps = listOf(TranslationQaIssue::state),
        allowedValues = mapOf(TranslationQaIssue::state to QaIssueState.OPEN),
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
      arguments
        .firstOrNull()
        ?.type
        ?.classifier as? KClass<*>

    val itemType =
      arguments[1]
        .type
        ?.classifier as? KClass<*>

    return groupType to itemType
  }

  companion object {
    fun getOrderedTypes(): List<ActivityGroupType> {
      val withType = ActivityGroupType.entries.filter { it.orderAfter != null }
      val referenced = withType.mapNotNull { it.orderAfter }
      val all = (withType + referenced).toSet()
      return all.sortedWith { a, b ->
        when {
          a.orderAfter == b -> 1
          b.orderAfter == a -> -1
          else -> 0
        }
      }
    }
  }
}
