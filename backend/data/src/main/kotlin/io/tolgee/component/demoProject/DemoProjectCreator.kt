package io.tolgee.component.demoProject

import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.BigMetaDto
import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.LanguageService
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.ApplicationContext

class DemoProjectCreator(
  private val organization: Organization,
  private val applicationContext: ApplicationContext,
) {
  fun createDemoProject(): Project {
    activityHolder.activity = ActivityType.CREATE_PROJECT
    activityHolder.activityRevision.projectId = project.id
    setStates()
    addBigMeta()
    return project
  }

  val project: Project by lazy {
    val project = Project().apply {
      name = "Demo project"
      this@apply.organizationOwner = organization
      this.description = "This is a demo project of an packing list app"
    }
    projectService.save(project)
    serAvatar(project)
    project
  }

  private val translations by lazy {
    DemoProjectData.translations.flatMap { (languageTag, translations) ->
      translations.map { (key, text) ->
        setTranslation(key, languageTag, text)
      }
    }.associateBy { it.language.tag to it.key.name }
  }

  private fun addBigMeta() {
    val bigMetaDto = BigMetaDto().apply {
      keys.forEach { (keyName, _) ->
        relatedKeysInOrder.add(
          RelatedKeyDto().apply {
            this.keyName = keyName
          }
        )
      }
    }
    bigMetaService.store(bigMetaDto, project)
  }

  private fun setStates() {
    DemoProjectData.inTranslatedState.forEach { (languageTag, key) ->
      translations[languageTag to key]!!.state = TranslationState.TRANSLATED
    }
  }

  private fun setTranslation(keyName: String, languageTag: String, translation: String): Translation {
    val language = languages[languageTag]!!
    return translationService.setTranslation(getOrCreateKey(keyName), language, translation).also {
      it.state = TranslationState.REVIEWED
    }
  }

  val keys: MutableMap<String, Key> = mutableMapOf()

  private fun getOrCreateKey(keyName: String): Key {
    return keys.computeIfAbsent(keyName) {
      val key = Key().apply {
        name = keyName
        this@apply.project = this@DemoProjectCreator.project
      }
      keyService.save(key)
      key
    }
  }

  private val languages: Map<String, Language> by lazy {
    DemoProjectData.languages.associateBy {
      it.project = project
      languageService.save(it)
      it.tag
    }
  }

  private fun serAvatar(project: Project) {
    applicationContext.getResource("classpath:demoProject/demoAvatar.png").inputStream.use {
      projectService.setAvatar(project, it)
    }
  }

  private val activityHolder: ActivityHolder by lazy {
    applicationContext.getBean(ActivityHolder::class.java)
  }

  private val bigMetaService: BigMetaService by lazy {
    applicationContext.getBean(BigMetaService::class.java)
  }

  private val languageService: LanguageService by lazy {
    applicationContext.getBean(LanguageService::class.java)
  }

  private val keyService: KeyService by lazy {
    applicationContext.getBean(KeyService::class.java)
  }

  private val translationService: TranslationService by lazy {
    applicationContext.getBean(TranslationService::class.java)
  }

  private val projectService: ProjectService by lazy {
    applicationContext.getBean(ProjectService::class.java)
  }
}
