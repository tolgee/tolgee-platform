package io.tolgee.development

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.ApiKey
import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.ApiKeyRepository
import io.tolgee.repository.OrganizationRepository
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.LanguageService
import io.tolgee.service.OrganizationRoleService
import io.tolgee.service.PermissionService
import io.tolgee.service.UserAccountService
import io.tolgee.util.SlugGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityManager

@Service
class DbPopulatorReal(
  private val entityManager: EntityManager,
  private val userAccountRepository: UserAccountRepository,
  private val permissionService: PermissionService,
  private val userAccountService: UserAccountService,
  private val languageService: LanguageService,
  private val projectRepository: ProjectRepository,
  private val apiKeyRepository: ApiKeyRepository,
  private val tolgeeProperties: TolgeeProperties,
  private val initialPasswordManager: InitialPasswordManager,
  private val organizationRepository: OrganizationRepository,
  private val slugGenerator: SlugGenerator,
  private val organizationRoleService: OrganizationRoleService
) {
  private lateinit var de: Language
  private lateinit var en: Language

  @Transactional
  fun autoPopulate() {
    // do not populate if db is not empty
    if (userAccountRepository.count() == 0L) {
      this.populate("Application")
    }
  }

  fun createUserIfNotExists(username: String, password: String? = null, name: String? = null): UserAccount {
    return userAccountService.findOptional(username).orElseGet {
      val signUpDto = SignUpDto(
        name = name ?: username, email = username,
        password = password
          ?: initialPasswordManager.initialPassword
      )
      userAccountService.createUser(signUpDto)
    }
  }

  fun createOrganization(name: String, userAccount: UserAccount): Organization {
    val slug = slugGenerator.generate(name, 3, 100) { true }
    val organization = Organization(name = name, slug = slug, basePermissions = Permission.ProjectPermissionType.VIEW)
    return organizationRepository.save(organization).also {
      organizationRoleService.grantOwnerRoleToUser(userAccount, organization)
    }
  }

  @Transactional
  fun createUsersAndOrganizations(username: String = "user", userCount: Int = 4): List<UserAccount> {
    val users = (1..userCount).map {
      createUserIfNotExists("$username $it")
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
  fun createProjectWithOrganization(projectName: String, organization: Organization): Project {
    val project = Project()
    project.name = projectName
    project.organizationOwner = organization
    project.slug = slugGenerator.generate(projectName, 3, 60) { true }
    en = createLanguage("en", project)
    de = createLanguage("de", project)
    projectRepository.saveAndFlush(project)
    organization.projects.add(project)
    entityManager.flush()
    entityManager.clear()
    return project
  }

  @Transactional
  fun createBase(projectName: String, username: String, password: String? = null): Project {
    val userAccount = createUserIfNotExists(username, password)
    val project = Project()
    project.name = projectName
    project.userOwner = userAccount
    en = createLanguage("en", project)
    project.baseLanguage = en
    de = createLanguage("de", project)
    permissionService.grantFullAccessToProject(userAccount, project)
    projectRepository.saveAndFlush(project)
    entityManager.flush()
    entityManager.clear()
    return project
  }

  @Transactional
  fun createBase(projectName: String, username: String): Project {
    return createBase(projectName, username, null)
  }

  @Transactional
  fun createBase(projectName: String): Project {
    return createBase(projectName, tolgeeProperties.authentication.initialUsername)
  }

  @Transactional
  fun populate(projectName: String): Project {
    return populate(projectName, tolgeeProperties.authentication.initialUsername)
  }

  @Transactional
  fun populate(projectName: String, userName: String): Project {
    val project = createBase(projectName, userName)
    createApiKey(project)
    createTranslation(project, "Hello world!", "Hallo Welt!", en, de)
    createTranslation(project, "English text one.", "Deutsch text einz.", en, de)
    createTranslation(
      project, "This is translation in home folder",
      "Das ist die Übersetzung im Home-Ordner", en, de
    )
    createTranslation(
      project, "This is translation in news folder",
      "Das ist die Übersetzung im News-Ordner", en, de
    )
    createTranslation(
      project, "This is another translation in news folder",
      "Das ist eine weitere Übersetzung im Nachrichtenordner", en, de
    )
    createTranslation(
      project, "This is standard text somewhere in DOM.",
      "Das ist Standardtext irgendwo im DOM.", en, de
    )
    createTranslation(
      project, "This is another standard text somewhere in DOM.",
      "Das ist ein weiterer Standardtext irgendwo in DOM.", en, de
    )
    createTranslation(
      project, "This is translation retrieved by service.",
      "Dase Übersetzung wird vom Service abgerufen.", en, de
    )
    createTranslation(
      project, "This is textarea with placeholder and value.",
      "Das ist ein Textarea mit Placeholder und Value.", en, de
    )
    createTranslation(
      project, "This is textarea with placeholder.",
      "Das ist ein Textarea mit Placeholder.", en, de
    )
    createTranslation(
      project, "This is input with value.",
      "Das ist ein Input mit value.", en, de
    )
    createTranslation(
      project, "This is input with placeholder.",
      "Das ist ein Input mit Placeholder.", en, de
    )
    return project
  }

  private fun createApiKey(project: Project) {
    val user = project.permissions.stream().findAny().orElseThrow { NotFoundException() }.user
    if (apiKeyRepository.findByKey(API_KEY).isEmpty) {
      val apiKey = ApiKey(
        project = project,
        key = API_KEY,
        userAccount = user!!,
        scopesEnum = ApiScope.values().toSet()
      )
      project.apiKeys.add(apiKey)
      apiKeyRepository.save(apiKey)
    }
  }

  private fun createLanguage(name: String, project: Project): Language {
    return languageService.createLanguage(LanguageDto(name, name, name), project)
  }

  private fun createTranslation(
    project: Project,
    english: String,
    deutsch: String,
    en: Language,
    de: Language
  ) {
    val key = Key()
    key.name = "sampleApp." + english.replace(" ", "_")
      .lowercase(Locale.getDefault()).replace("\\.+$".toRegex(), "")
    key.project = project
    val translation = Translation()
    translation.language = en
    translation.key = key
    translation.text = english
    key.translations.add(translation)
    key.translations.add(translation)
    entityManager.persist(key)
    entityManager.persist(translation)
    val translationDe = Translation()
    translationDe.language = de
    translationDe.key = key
    translationDe.text = deutsch
    key.translations.add(translationDe)
    entityManager.persist(translationDe)
    entityManager.flush()
  }

  companion object {
    const val API_KEY = "this_is_dummy_api_key"
  }
}
