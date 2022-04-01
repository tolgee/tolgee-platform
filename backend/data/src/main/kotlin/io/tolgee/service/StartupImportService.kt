package io.tolgee.service

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.dtos.request.project.CreateProjectDTO
import io.tolgee.model.ApiKey
import io.tolgee.model.enums.ApiScope
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.dataImport.ImportService
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
  private val projectHolder: ProjectHolder
) {

  @Transactional
  fun importFiles() {
    val dir = properties.import.dir

    if (dir !== null && File(dir).exists() && File(dir).isDirectory) {
      File(dir).listFiles()?.filter { it.isDirectory }?.forEach { projectDir ->
        val fileDtos = projectDir.listFiles()?.map { it -> ImportFileDto(it.name, it.inputStream()) }?.toList()

        if (fileDtos != null) {
          val userAccount = userAccountService
            .findOptional(properties.authentication.initialUsername)
            .orElseGet { null }

          if (userAccount != null) {
            val projectName = projectDir.nameWithoutExtension
            val existingProjects = projectService.findAllByNameAndUserOwner(projectName, userAccount)
            if (existingProjects.isEmpty()) {
              val project = projectService.createProject(
                CreateProjectDTO(
                  name = projectName,
                  languages = fileDtos.map { file ->
                    // remove extension
                    val name = file.name.replace(Regex("\\.[^.]*"), "")
                    LanguageDto(name, name, name)
                  }
                ),
                userAccount
              )

              if (properties.import.createImplicitApiKey) {
                val apiKey = ApiKey(
                  key = "${projectName.lowercase(Locale.getDefault())}-${userAccount.name}-imported-project-implicit",
                  scopesEnum = ApiScope.values().toMutableSet(),
                  userAccount = userAccount,
                  project = project
                )
                apiKeyService.save(apiKey)
                project.apiKeys.add(apiKey)
              }

              projectService.save(project)
              projectHolder.project = ProjectDto.fromEntity(project)
              importService.addFiles(fileDtos, null, project, userAccount)
              val imports = importService.getAllByProject(project.id)
              imports.forEach {
                importService.import(it)
              }
            }
          }
        }
      }
    }
  }
}
