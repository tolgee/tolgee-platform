package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.development.testDataBuilder.builders.slack.SlackConfigBuilder
import io.tolgee.model.*
import io.tolgee.model.automations.Automation
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportSettings
import io.tolgee.model.key.Key
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.keyBigMeta.KeysDistance
import io.tolgee.model.mtServiceConfig.MtServiceConfig
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskKey
import io.tolgee.model.translation.Translation
import io.tolgee.model.webhook.WebhookConfig
import org.springframework.core.io.ClassPathResource

class ProjectBuilder(
  organizationOwner: Organization? = null,
  val testDataBuilder: TestDataBuilder,
) : BaseEntityDataBuilder<Project, ProjectBuilder>() {
  override var self: Project =
    Project().apply {
      if (organizationOwner == null) {
        if (testDataBuilder.data.organizations.size > 0) {
          this.organizationOwner = testDataBuilder.data.organizations.first().self
        }
        return@apply
      }
      this.organizationOwner = organizationOwner
    }

  class DATA {
    val permissions = mutableListOf<PermissionBuilder>()
    val languages = mutableListOf<LanguageBuilder>()
    val imports = mutableListOf<ImportBuilder>()
    val keys = mutableListOf<KeyBuilder>()
    val translations = mutableListOf<TranslationBuilder>()
    val automations = mutableListOf<AutomationBuilder>()
    val apiKeys = mutableListOf<ApiKeyBuilder>()
    val translationServiceConfigs = mutableListOf<MtServiceConfigBuilder>()
    var autoTranslationConfigBuilders = mutableListOf<AutoTranslationConfigBuilder>()
    var avatarFile: ClassPathResource? = null
    var namespaces = mutableListOf<NamespaceBuilder>()
    var keyScreenshotReferences = mutableListOf<KeyScreenshotReferenceBuilder>()
    var screenshots = mutableListOf<ScreenshotBuilder>()
    var keyDistances = mutableListOf<KeysDistanceBuilder>()
    var contentStorages = mutableListOf<ContentStorageBuilder>()
    var contentDeliveryConfigs = mutableListOf<ContentDeliveryContentBuilder>()
    var webhookConfigs = mutableListOf<WebhookConfigBuilder>()
    var importSettings: ImportSettings? = null
    var slackConfigs = mutableListOf<SlackConfigBuilder>()
    val batchJobs: MutableList<BatchJobBuilder> = mutableListOf()
    val tasks = mutableListOf<TaskBuilder>()
    val taskKeys = mutableListOf<TaskKeyBuilder>()
    val prompts = mutableListOf<PromptBuilder>()
    val aiPlaygroundResults = mutableListOf<AiPlaygroundResultBuilder>()
  }

  var data = DATA()

  fun addPermission(ft: FT<Permission>) = addOperation(data.permissions, ft)

  fun addApiKey(ft: FT<ApiKey>) = addOperation(data.apiKeys, ft)

  fun addImport(ft: FT<Import> = {}) = addOperation(data.imports, ImportBuilder(this), ft)

  fun addLanguage(ft: FT<Language>) = addOperation(data.languages, ft)

  fun addKey(ft: FT<Key>) = addOperation(data.keys, ft)

  fun addTask(ft: FT<Task>) = addOperation(data.tasks, ft)

  fun addTaskKey(ft: FT<TaskKey>) = addOperation(data.taskKeys, ft)

  fun addKey(
    namespace: String? = null,
    keyName: String,
    ft: (KeyBuilder.() -> Unit)? = null,
  ): KeyBuilder {
    return addKey(keyName, ft).build { setNamespace(namespace) }
  }

  fun addKey(keyName: String): KeyBuilder {
    return addKey(keyName, null)
  }

  fun addKey(
    keyName: String,
    ft: (KeyBuilder.() -> Unit)?,
  ): KeyBuilder {
    return addKey {
      name = keyName
    }.apply {
      ft?.let {
        apply(it)
      }
    }
  }

  fun addTranslation(ft: FT<Translation>) = addOperation(data.translations, ft)

  fun addMtServiceConfig(ft: FT<MtServiceConfig>) = addOperation(data.translationServiceConfigs, ft)

  fun setAvatar(filePath: String) {
    data.avatarFile = ClassPathResource(filePath, this.javaClass.classLoader)
  }

  fun addAutoTranslationConfig(ft: FT<AutoTranslationConfig>) {
    addOperation(data.autoTranslationConfigBuilders, ft)
  }

  fun addNamespace(ft: FT<Namespace>) = addOperation(data.namespaces, ft)

  fun addScreenshotReference(ft: FT<KeyScreenshotReference>) = addOperation(data.keyScreenshotReferences, ft)

  fun addScreenshot(ft: FT<Screenshot>) = addOperation(data.screenshots, ft)

  fun addEnglish(): LanguageBuilder {
    return addLanguage {
      name = "English"
      tag = "en"
    }
  }

  fun addGerman(): LanguageBuilder {
    return addLanguage {
      name = "German"
      originalName = "Deutsch"
      tag = "de"
    }
  }

  fun addCzech(): LanguageBuilder {
    return addLanguage {
      name = "Czech"
      originalName = "Čeština"
      tag = "cs"
    }
  }

  fun addHindi(): LanguageBuilder {
    return addLanguage {
      name = "Hindi"
      originalName = "हिन्दी"
      tag = "hi"
    }
  }

  fun addFrench(): LanguageBuilder {
    return addLanguage {
      name = "French"
      originalName = "Français"
      tag = "fr"
    }
  }

  fun getLanguageByTag(tag: String): LanguageBuilder? {
    return data.languages.find { it.self.tag == tag }
  }

  fun addKeysDistance(
    key1: Key,
    key2: Key,
    ft: FT<KeysDistance>,
  ): KeysDistanceBuilder {
    val builder = KeysDistanceBuilder(this, key1, key2)
    ft(builder.self)
    data.keyDistances.add(builder)
    return builder
  }

  fun addAutomation(ft: FT<Automation>) = addOperation(data.automations, ft)

  fun addContentStorage(ft: FT<ContentStorage>) = addOperation(data.contentStorages, ft)

  fun addContentDeliveryConfig(ft: FT<ContentDeliveryConfig>) = addOperation(data.contentDeliveryConfigs, ft)

  fun addWebhookConfig(ft: FT<WebhookConfig>) = addOperation(data.webhookConfigs, ft)

  fun addSlackConfig(ft: FT<SlackConfig>) = addOperation(data.slackConfigs, ft)

  fun addBatchJob(ft: FT<BatchJob>) = addOperation(data.batchJobs, ft)

  fun setImportSettings(ft: FT<ImportSettings>) {
    data.importSettings = ImportSettings(this.self).apply(ft)
  }

  fun addPrompt(ft: FT<Prompt>) = addOperation(data.prompts, ft)

  fun addAiPlaygroundResult(ft: FT<AiPlaygroundResult>) = addOperation(data.aiPlaygroundResults, ft)

  val onlyUser get() = this.self.organizationOwner.memberRoles.singleOrNull()?.user

  fun getTranslation(
    key: Key,
    languageTag: String,
  ): Translation? {
    return this.data.translations.find { it.self.key == key && it.self.language.tag == languageTag }?.self
  }
}
