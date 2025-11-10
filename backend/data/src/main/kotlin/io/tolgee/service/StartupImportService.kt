package io.tolgee.service

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.model.ApiKey
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.project.ProjectCreationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.util.Locale

@Service
class StartupImportService(
  private val importService: ImportService,
  private val projectService: ProjectService,
  private val userAccountService: UserAccountService,
  private val properties: TolgeeProperties,
  private val apiKeyService: ApiKeyService,
  private val applicationContext: ApplicationContext,
  private val entityManager: EntityManager,
  private val projectCreationService: ProjectCreationService,
) : Logging {
  @Transactional
  fun importFiles() {
    getDirsToImport()?.forEach { projectDir ->
      val fileDtos = getImportFileDtos(projectDir)
      val projectName = projectDir.nameWithoutExtension
      importAsInitialUserProject(projectName, fileDtos)
    }
  }

  private fun importAsInitialUserProject(
    projectName: String,
    fileDtos: List<ImportFileDto>,
  ) {
    val userAccount = getInitialUserAccount() ?: return
    val organization = getInitialUserOrganization(userAccount)
    setAuthentication(userAccount)
    if (!isProjectExisting(projectName, organization)) {
      logger.info("Importing initial project $projectName [user: $userAccount, organization: $organization]")
      val project = createProject(projectName, fileDtos, organization)
      createImplicitApiKey(userAccount, project)
      assignProjectHolder(project)
      importData(fileDtos, project, userAccount)
      disableNamespacesIfNoneWereImported(project.id)
      return
    }
    logger.info("Not Importing initial project $projectName - project already exists")
  }

  private fun getInitialUserOrganization(userAccount: UserAccount?) =
    userAccount?.organizationRoles?.singleOrNull()?.organization
      ?: throw IllegalStateException("No initial organization")

  private fun getImportFileDtos(projectDir: File) =
    projectDir
      .walk()
      .filter { !it.isDirectory }
      .map {
        val relativePath = it.path.replace(projectDir.path, "")
        if (relativePath.isBlank()) null else ImportFileDto(relativePath, it.readBytes())
      }.filterNotNull()
      .toList()

  private fun setAuthentication(userAccount: UserAccount) {
    SecurityContextHolder.getContext().authentication =
      TolgeeAuthentication(
        credentials = null,
        deviceId = null,
        userAccount = UserAccountDto.fromEntity(userAccount),
        actingAsUserAccount = null,
        isReadOnly = false,
        isSuperToken = false,
      )
  }

  private fun isProjectExisting(
    projectName: String,
    organization: Organization,
  ) = projectService.findAllByNameAndOrganizationOwner(projectName, organization).isNotEmpty()

  private fun getDirsToImport(): List<File>? {
    properties.import.dir?.let { dir ->
      val file = File(dir)
      if (file.exists() && file.isDirectory) {
        return file.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }
      }
    }
    return null
  }

  private fun importData(
    fileDtos: List<ImportFileDto>,
    project: Project,
    userAccount: UserAccount,
  ) {
    importService.addFiles(
      files = fileDtos,
      project = project,
      userAccount = userAccount,
      params = ImportAddFilesParams(storeFilesToFileStorage = false),
    )
    entityManager.flush()
    entityManager.clear()
    val imports = importService.getAllByProject(project.id)
    imports.forEach {
      importService.import(it)
    }
  }

  private fun assignProjectHolder(project: Project) {
    applicationContext
      .getBean(
        "transactionProjectHolder",
        ProjectHolder::class.java,
      ).project = ProjectDto.fromEntity(project)
  }

  private fun createProject(
    projectName: String,
    fileDtos: List<ImportFileDto>,
    organization: Organization,
  ): Project {
    val languages =
      fileDtos
        .map { file ->
          // remove extension
          val name = getLanguageName(file)
          LanguageRequest(name, name, name)
        }.toSet()
        .toList()

    val project =
      projectCreationService.createProject(
        CreateProjectRequest(
          name = projectName,
          languages = languages,
          organizationId = organization.id,
        ),
      )

    setBaseLanguage(project)
    project.useNamespaces = true

    projectService.save(project)
    return project
  }

  private fun setBaseLanguage(project: Project) {
    project.languages.find { it.tag == properties.import.baseLanguageTag }?.let {
      project.baseLanguage = it
    } ?: let {
      logger.warn("Base language ${properties.import.baseLanguageTag} not found for project ${project.name}")
    }
  }

  private fun getLanguageName(file: ImportFileDto): String {
    val name = file.name.replace(Regex("^.*/([a-zA-Z0-9_\\-]+)\\.\\w+\$"), "$1")
    if (name.isBlank()) {
      throw IllegalStateException("File name is blank")
    }
    return name
  }

  private fun createImplicitApiKey(
    userAccount: UserAccount,
    project: Project,
  ) {
    if (properties.import.createImplicitApiKey) {
      val apiKey =
        ApiKey(
          key = "${project.name.lowercase(Locale.getDefault())}-${userAccount.name}-imported-project-implicit",
          scopesEnum = Scope.values().toMutableSet(),
          userAccount = userAccount,
          project = project,
        )
      apiKeyService.save(apiKey)
      project.apiKeys.add(apiKey)
    }
  }

  private fun getInitialUserAccount(): UserAccount? {
    val userAccount =
      userAccountService
        .findActive(properties.authentication.initialUsername)
    return userAccount
  }

  private fun disableNamespacesIfNoneWereImported(projectId: Long) {
    val project = projectService.find(projectId)!!

    if (project.namespaces.isNotEmpty()) {
      return
    }

    project.useNamespaces = false
    projectService.save(project)
  }
}
