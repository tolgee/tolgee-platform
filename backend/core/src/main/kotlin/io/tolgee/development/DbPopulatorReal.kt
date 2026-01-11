package io.tolgee.development

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.model.ApiKey
import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.LanguageStatsService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.SlugGenerator
import io.tolgee.util.executeInNewTransaction
import jakarta.persistence.EntityManager
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.util.Locale
import java.util.UUID

@Service
class DbPopulatorReal(
  private val entityManager: EntityManager,
  private val userAccountService: UserAccountService,
  private val languageService: LanguageService,
  private val tolgeeProperties: TolgeeProperties,
  private val initialPasswordManager: InitialPasswordManager,
  private val slugGenerator: SlugGenerator,
  private val organizationRoleService: OrganizationRoleService,
  private val namespaceService: NamespaceService,
  private val projectService: ProjectService,
  private val organizationService: OrganizationService,
  private val apiKeyService: ApiKeyService,
  private val languageStatsService: LanguageStatsService,
  private val platformTransactionManager: PlatformTransactionManager,
  private val passwordEncoder: PasswordEncoder,
) {
  private lateinit var de: Language
  private lateinit var en: Language

  @Transactional
  fun autoPopulate() {
    // do not populate if db is not empty
    if (userAccountService.countAll() == 0L) {
      this.populate()
    }
  }

  fun createUserIfNotExists(
    username: String,
    password: String? = null,
    name: String? = null,
  ): UserAccount {
    return userAccountService.findActive(username) ?: let {
      val rawPassword =
        password
          ?: initialPasswordManager.initialPassword

      userAccountService.createUser(
        UserAccount(
          name = name ?: username,
          username = username,
          password = passwordEncoder.encode(rawPassword),
        ),
      )
    }
  }

  fun createOrganization(
    name: String,
    userAccount: UserAccount,
  ): Organization {
    val slug = slugGenerator.generate(name, 3, 100) { true }
    return organizationService.create(OrganizationDto(name, slug), userAccount)
  }

  @Transactional
  @Deprecated("Use create PermissionTestData or other permission classes")
  fun createUsersAndOrganizations(
    username: String = "user",
    userCount: Int = 4,
  ): List<UserAccount> {
    val users =
      (1..userCount).map {
        createUserIfNotExists("$username-$it")
      }

    users.mapIndexed { listUserIdx, user ->
      (1..listUserIdx).forEach { organizationNum ->
        val org = createOrganization("${user.name}'s organization $organizationNum", user)
        (0 until listUserIdx).forEach { userIdx ->
          organizationRoleService.grantRoleToUser(users[userIdx], org, OrganizationRoleType.MEMBER)
        }
        (1..3).forEach { projectNum ->
          val name = "${user.name}'s organization $organizationNum project $projectNum"
          createProjectWithOrganization(name, org)
        }
      }
    }

    return users
  }

  @Transactional
  fun createProjectWithOrganization(
    projectName: String,
    organization: Organization,
  ): Project {
    val project = Project()
    project.name = projectName
    project.organizationOwner = organization
    project.slug = slugGenerator.generate(projectName, 3, 60) { true }
    en = createLanguage("en", project)
    project.baseLanguage = en
    de = createLanguage("de", project)
    organization.projects.add(project)
    projectService.save(project)
    return project
  }

  @Transactional
  fun createBase(
    projectName: String,
    username: String,
    password: String? = null,
  ): Base {
    val userAccount = createUserIfNotExists(username, password)
    val organization = createOrganizationIfNotExist(username, username, userAccount)
    val project = createProject(projectName, organization)
    return Base(project, organization, userAccount)
  }

  fun createProject(
    projectName: String,
    organization: Organization,
  ): Project {
    val project = Project()
    project.name = projectName
    project.organizationOwner = organization
    projectService.save(project)
    en = createLanguage("en", project)
    project.baseLanguage = en
    de = createLanguage("de", project)
    projectService.save(project)
    entityManager.flush()
    entityManager.clear()
    return project
  }

  fun createOrganizationIfNotExist(
    name: String,
    slug: String = name,
    userAccount: UserAccount,
  ): Organization {
    return organizationService.find(name) ?: let {
      organizationService.create(OrganizationDto(name, slug = slug), userAccount)
    }
  }

  @Transactional
  fun createBase(username: String): Base {
    return createBase(UUID.randomUUID().toString(), username, null)
  }

  @Transactional
  fun createBase(): Base {
    return createBase(tolgeeProperties.authentication.initialUsername)
  }

  fun populate(): Base {
    return executeInNewTransaction(platformTransactionManager) {
      populate(userName = tolgeeProperties.authentication.initialUsername)
    }.also {
      languageStatsService.refreshLanguageStats(it.project.id)
    }
  }

  @Transactional
  fun populate(userName: String): Base {
    val base = createBase(userName)
    val project = projectService.get(base.project.id)
    createApiKey(project)
    createTranslation(project, "Hello world!", "Hallo Welt!", en, de)
    createTranslation(project, "English text one.", "Deutsch text einz.", en, de)
    createTranslation(
      project,
      "This is translation in home folder",
      "Das ist die Übersetzung im Home-Ordner",
      en,
      de,
    )
    createTranslation(
      project,
      "This is translation in news folder",
      "Das ist die Übersetzung im News-Ordner",
      en,
      de,
    )
    createTranslation(
      project,
      "This is another translation in news folder",
      "Das ist eine weitere Übersetzung im Nachrichtenordner",
      en,
      de,
    )
    createTranslation(
      project,
      "This is standard text somewhere in DOM.",
      "Das ist Standardtext irgendwo im DOM.",
      en,
      de,
    )
    createTranslation(
      project,
      "This is another standard text somewhere in DOM.",
      "Das ist ein weiterer Standardtext irgendwo in DOM.",
      en,
      de,
    )
    createTranslation(
      project,
      "This is translation retrieved by service.",
      "Dase Übersetzung wird vom Service abgerufen.",
      en,
      de,
    )
    createTranslation(
      project,
      "This is textarea with placeholder and value.",
      "Das ist ein Textarea mit Placeholder und Value.",
      en,
      de,
    )
    createTranslation(
      project,
      "This is textarea with placeholder.",
      "Das ist ein Textarea mit Placeholder.",
      en,
      de,
    )
    createTranslation(
      project,
      "This is input with value.",
      "Das ist ein Input mit value.",
      en,
      de,
    )
    createTranslation(
      project,
      "This is input with placeholder.",
      "Das ist ein Input mit Placeholder.",
      en,
      de,
    )
    return base
  }

  private fun createApiKey(project: Project) {
    val user = project.organizationOwner.memberRoles[0].user
    if (apiKeyService.findOptional(apiKeyService.hashKey(API_KEY)).isEmpty) {
      val apiKey =
        ApiKey(
          project = project,
          key = API_KEY,
          userAccount = user!!,
          scopesEnum = Scope.values().toSet(),
        )
      project.apiKeys.add(apiKey)
      apiKeyService.save(apiKey)
    }
  }

  private fun createLanguage(
    name: String,
    project: Project,
  ): Language {
    return languageService.createLanguage(LanguageRequest(name, name, name), project)
  }

  private fun createTranslation(
    project: Project,
    english: String,
    deutsch: String,
    en: Language,
    de: Language,
  ) {
    val key = Key()
    key.name = "sampleApp." +
      english
        .replace(" ", "_")
        .lowercase(Locale.getDefault())
        .replace("\\.+$".toRegex(), "")
    key.project = project
    val translation = Translation()
    translation.language = en
    translation.key = key
    translation.text = english
    entityManager.persist(key)
    entityManager.persist(translation)
    val translationDe = Translation()
    translationDe.language = de
    translationDe.key = key
    translationDe.text = deutsch
    entityManager.persist(translationDe)
    entityManager.flush()
  }

  fun createNamespace(
    project: Project,
    name: String = UUID.randomUUID().toString(),
  ) {
    namespaceService.create(name, project.id)
  }

  companion object {
    const val API_KEY = "this_is_dummy_api_key"
  }
}
