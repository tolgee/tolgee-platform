package io.tolgee.component.demoProject

import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.dtos.request.KeyInScreenshotPositionDto
import io.tolgee.dtos.request.ScreenshotInfoDto
import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.key.TagService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import io.tolgee.service.translation.TranslationCommentService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.ApplicationContext
import java.awt.Dimension

class DemoProjectCreator(
  private val organization: Organization,
  private val applicationContext: ApplicationContext,
) {
  companion object {
    const val SCREENSHOT_WIDTH = 2387
    const val SCREENSHOT_HEIGHT = 1256
  }

  fun createDemoProject(): Project {
    activityHolder.activity = ActivityType.CREATE_PROJECT
    activityHolder.activityRevision.projectId = project.id
    setStates()
    addBigMeta()
    addScreenshots()
    tagKeys()
    setDescriptions()
    addComments()
    project.baseLanguage = languages["en"]
    projectService.save(project)
    return project
  }

  private fun addComments() {
    DemoProjectData.comments.forEach { comment ->
      val translation = translations[comment.language to comment.key]
      val translationComment =
        TranslationComment(
          text = comment.text,
          state = TranslationCommentState.RESOLVED,
          translation = translation!!,
        ).also {
          it.author = users[comment.author.username]!!
        }
      translationCommentService.create(translationComment)
    }
  }

  val project: Project by lazy {
    val project =
      Project().apply {
        name = "Demo project"
        this@apply.organizationOwner = organization
        this.description = "This is a demo project of a packing list app"
      }
    projectService.save(project)
    setAvatar(project)
    project
  }

  /**
   * Map of Pair(languageTag, keyName) -> Translation
   */
  private val translations by lazy {
    DemoProjectData.translations
      .flatMap { (languageTag, translations) ->
        translations.map { (key, text) ->
          setTranslation(key, languageTag, text)
        }
      }.associateBy { it.language.tag to it.key.name }
  }

  private fun addBigMeta() {
    val relatedKeysInOrder = mutableListOf<RelatedKeyDto>()
    keys.forEach { (keyName, _) ->
      relatedKeysInOrder.add(
        RelatedKeyDto().apply {
          this.keyName = keyName
        },
      )
    }
    bigMetaService.store(relatedKeysInOrder, project)
  }

  private fun setStates() {
    DemoProjectData.inTranslatedState.forEach { (languageTag, key) ->
      translations[languageTag to key]!!.state = TranslationState.TRANSLATED
    }
  }

  private fun setTranslation(
    keyName: String,
    languageTag: String,
    translation: String,
  ): Translation {
    val language = languages[languageTag]!!
    return translationService.setTranslationText(getOrCreateKey(keyName), language, translation).also {
      it.state = TranslationState.REVIEWED
    }
  }

  private fun tagKeys() {
    val tagsMap =
      DemoProjectData.tags
        .mapNotNull {
          val key = keys[it.key] ?: return@mapNotNull null
          key to it.value
        }.toMap()
    tagService.tagKeys(tagsMap)
  }

  private fun addScreenshots() {
    val screenshot = saveScreenshot()

    DemoProjectData.screenshots.forEach { demoScreenshot ->
      val key = getOrCreateKey(demoScreenshot.keyName)

      val positions =
        demoScreenshot.positions.map {
          KeyInScreenshotPositionDto(it.x, it.y, it.width, it.height)
        }

      val dimension = Dimension(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT)
      screenshotService.addReference(
        key,
        screenshot,
        ScreenshotInfoDto(text = null, positions, null),
        dimension,
        dimension,
      )
    }
  }

  private fun saveScreenshot(): Screenshot {
    val image =
      applicationContext
        .getResource("classpath:demoProject/screenshot.png")
        .inputStream
        .use { it.readAllBytes() }
    val middleSized =
      applicationContext
        .getResource("classpath:demoProject/screenshot-middle-sized.png")
        .inputStream
        .use { it.readAllBytes() }
    val thumbnail =
      applicationContext
        .getResource("classpath:demoProject/screenshot-thumbnail.png")
        .inputStream
        .use { screenshotThumbnail -> screenshotThumbnail.readAllBytes() }

    return screenshotService.saveScreenshot(
      image,
      middleSized,
      thumbnail,
      null,
      Dimension(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT),
    )
  }

  val keys: MutableMap<String, Key> = mutableMapOf()

  private fun getOrCreateKey(keyName: String): Key {
    return keys.computeIfAbsent(keyName) {
      val key =
        Key().apply {
          name = keyName
          val pluralArgName = DemoProjectData.pluralArgNames[name]
          if (pluralArgName != null) {
            isPlural = true
            this.pluralArgName = pluralArgName
          }
          this@apply.project = this@DemoProjectCreator.project
        }
      keyService.save(key)
      key
    }
  }

  private fun setDescriptions() {
    DemoProjectData.descriptions.forEach { (keyName, description) ->
      val key = getOrCreateKey(keyName)
      val meta = getOrCreateKeyMeta(key)
      meta.description = description
      keyMetaService.save(meta)
    }
  }

  private fun getOrCreateKeyMeta(key: Key): KeyMeta {
    return key.keyMeta ?: let {
      val keyMeta = KeyMeta()
      keyMeta.key = key
      key.keyMeta = keyMeta
      keyMetaService.save(keyMeta)
      keyMeta
    }
  }

  private val languages: Map<String, Language> by lazy {
    DemoProjectData.languages.associateBy {
      it.project = project
      languageService.save(it)
      it.tag
    }
  }

  private fun setAvatar(project: Project) {
    applicationContext.getResource("classpath:demoProject/demoAvatar.png").inputStream.use {
      projectService.setAvatar(project, it)
    }
  }

  private val users by lazy {
    userAccountService.getOrCreateDemoUsers(DemoProjectData.demoUsers)
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

  private val screenshotService: ScreenshotService by lazy {
    applicationContext.getBean(ScreenshotService::class.java)
  }

  private val tagService: TagService by lazy {
    applicationContext.getBean(TagService::class.java)
  }

  private val keyMetaService: KeyMetaService by lazy {
    applicationContext.getBean(KeyMetaService::class.java)
  }

  private val userAccountService: UserAccountService by lazy {
    applicationContext.getBean(UserAccountService::class.java)
  }

  private val translationCommentService: TranslationCommentService by lazy {
    applicationContext.getBean(TranslationCommentService::class.java)
  }
}
