package io.tolgee.service

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.dtos.request.project.CreateProjectDTO
import io.tolgee.model.ApiKey
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ApiScope
import io.tolgee.security.AuthenticationProvider
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.UserAccountService
import org.springframework.context.ApplicationContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.util.*

@Service
class StartupImportService(
  private val importService: ImportService,
  private val projectService: ProjectService,
  private val userAccountService: UserAccountService,
  private val properties: TolgeeProperties,
  private val apiKeyService: ApiKeyService,
  private val applicationContext: ApplicationContext,
  private val authenticationProvider: AuthenticationProvider
) {

  @Transactional
  fun importFiles() {
    val dir = properties.import.dir

    if (dir !== null && File(dir).exists() && File(dir).isDirectory) {
      File(dir).listFiles()?.filter { it.isDirectory }?.forEach { projectDir ->
        val fileDtos = projectDir.listFiles()?.map { it -> ImportFileDto(it.name, it.inputStream()) }?.toList()

        if (fileDtos != null) {
          val userAccount = getInitialUserAccount()
          val organization = userAccount?.organizationRoles?.singleOrNull()?.organization ?: return
          SecurityContextHolder.getContext().authentication = authenticationProvider.getAuthentication(userAccount)
          val projectName = projectDir.nameWithoutExtension
          val existingProjects = projectService.findAllByNameAndOrganizationOwner(projectName, organization)
          if (existingProjects.isEmpty()) {
            val project = createProject(projectName, fileDtos, organization)
            createImplicitApiKey(userAccount, project)
            assignProjectHolder(project)
            importData(fileDtos, project, userAccount)
          }
        }
      }
    }
  }

  private fun importData(
    fileDtos: List<ImportFileDto>,
    project: Project,
    userAccount: UserAccount
  ) {
    importService.addFiles(fileDtos, project, userAccount)
    val imports = importService.getAllByProject(project.id)
    imports.forEach {
      importService.import(it)
    }
  }

  private fun assignProjectHolder(project: Project) {
    applicationContext.getBean(
      "transactionProjectHolder",
      ProjectHolder::class.java
    ).project = ProjectDto.fromEntity(project)
  }

  private fun createProject(
    projectName: String,
    fileDtos: List<ImportFileDto>,
    organization: Organization
  ): Project {
    val project = projectService.createProject(
      CreateProjectDTO(
        name = projectName,
        languages = fileDtos.map { file ->
          // remove extension
          val name = file.name.replace(Regex("\\.[^.]*"), "")
          LanguageDto(name, name, name)
        },
        organizationId = organization.id
      ),
    )
    projectService.save(project)
    return project
  }

  private fun createImplicitApiKey(
    userAccount: UserAccount,
    project: Project
  ) {
    if (properties.import.createImplicitApiKey) {
      val apiKey = ApiKey(
        key = "${project.name.lowercase(Locale.getDefault())}-${userAccount.name}-imported-project-implicit",
        scopesEnum = ApiScope.values().toMutableSet(),
        userAccount = userAccount,
        project = project
      )
      apiKeyService.save(apiKey)
      project.apiKeys.add(apiKey)
    }
  }

  private fun getInitialUserAccount(): UserAccount? {
    val userAccount = userAccountService
      .find(properties.authentication.initialUsername)
    return userAccount
  }
}
