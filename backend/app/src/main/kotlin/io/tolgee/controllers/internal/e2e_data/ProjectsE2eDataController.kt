package io.tolgee.controllers.internal.e2e_data

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.OrganizationRepository
import io.tolgee.repository.PermissionRepository
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import io.tolgee.security.InternalController
import io.tolgee.service.KeyService
import io.tolgee.service.LanguageService
import io.tolgee.service.OrganizationRoleService
import io.tolgee.service.OrganizationService
import io.tolgee.service.ProjectService
import io.tolgee.service.UserAccountService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/projects"])
@Transactional
@InternalController
class ProjectsE2eDataController(
  private val organizationService: OrganizationService,
  private val userAccountService: UserAccountService,
  private val organizationRoleService: OrganizationRoleService,
  private val organizationRepository: OrganizationRepository,
  private val projectService: ProjectService,
  private val projectRepository: ProjectRepository,
  private val userAccountRepository: UserAccountRepository,
  private val permissionRepository: PermissionRepository,
  private val keyService: KeyService,
  private val languageService: LanguageService,
) {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun createProjects() {
    val createdUsers = mutableMapOf<String, UserAccount>()

    users.forEach {
      createdUsers[it.email] = userAccountService.dtoToEntity(
        SignUpDto(
          name = it.name, email = it.email, password = "admin"
        )
      )
    }

    userAccountRepository.saveAll(createdUsers.values)

    organizations.forEach {
      val organization = organizationRepository.save(
        Organization(
          name = it.name,
          slug = organizationService.generateSlug(it.name),
          basePermissions = it.basePermission
        )
      )

      it.owners.forEach {
        createdUsers[it]!!.let { user ->
          organizationRoleService.grantOwnerRoleToUser(user, organization)
        }
      }

      it.members.forEach {
        createdUsers[it]!!.let { user ->
          organizationRoleService.grantMemberRoleToUser(user, organization)
        }
      }
    }

    projects.forEach { projectData ->

      val userOwner = if (projectData.userOwner != null)
        createdUsers[projectData.userOwner]!! else null

      val organizationOwner = if (projectData.organizationOwner != null)
        organizationService.find(projectData.organizationOwner) else null

      val project = projectRepository.save(
        Project(
          name = projectData.name,
          slug = projectService.generateSlug(projectData.name),
          userOwner = userOwner,
          organizationOwner = organizationOwner
        )
      )

      projectData.permittedUsers.forEach {
        val user = createdUsers[it.userName]!!
        permissionRepository.save(Permission(project = project, user = user, type = it.permission))
      }

      val createdLanguages = mutableListOf<String>()

      projectData.keyData.forEach {
        it.value.keys.forEach {
          if (!createdLanguages.contains(it)) {
            languageService.createLanguage(LanguageDto(name = it, tag = it), project)
            createdLanguages.add(it)
          }
        }
        keyService.create(project, SetTranslationsWithKeyDto(it.key, it.value))
      }
    }
  }

  @GetMapping(value = ["/clean"])
  @Transactional
  fun cleanupProjects() {
    projectService.deleteAllByName("I am a great project")

    projects.forEach {
      projectService.deleteAllByName(it.name)
    }

    organizations.forEach {
      organizationService.deleteAllByName(it.name)
    }

    users.forEach {
      userAccountService.findOptional(username = it.email).orElse(null)?.let {
        userAccountRepository.delete(it)
      }
    }
  }

  companion object {
    data class PermittedUserData(
      val userName: String,
      val permission: Permission.ProjectPermissionType,
    )

    data class UserData(
      val email: String,
      val name: String = email
    )

    data class OrganizationData(
      val basePermission: Permission.ProjectPermissionType,
      val name: String,
      val owners: MutableList<String> = mutableListOf(),
      val members: MutableList<String> = mutableListOf(),
    )

    data class ProjectDataItem(
      val name: String,
      val organizationOwner: String? = null,
      val userOwner: String? = null,
      val permittedUsers: MutableList<PermittedUserData> = mutableListOf(),
      val keyData: Map<String, Map<String, String>> = mutableMapOf()
    )

    val users = mutableListOf(
      UserData("gates@microsoft.com", "Bill Gates"),
      UserData("evan@netsuite.com", "Evan Goldberg"),
      UserData("cukrberg@facebook.com", "Mark Cukrberg"),
      UserData("vaclav.novak@fake.com", "Vaclav Novak"),
      UserData("john@doe.com", "John Doe"),
    )

    val organizations = mutableListOf(
      OrganizationData(
        name = "Facebook",
        basePermission = Permission.ProjectPermissionType.MANAGE,
        owners = mutableListOf("cukrberg@facebook.com"),
        members = mutableListOf("john@doe.com")
      ),
      OrganizationData(
        name = "Microsoft",
        basePermission = Permission.ProjectPermissionType.EDIT,
        owners = mutableListOf("gates@microsoft.com"),
        members = mutableListOf("john@doe.com", "cukrberg@facebook.com")
      )
    )

    val projects = mutableListOf(
      ProjectDataItem(
        name = "Facebook itself",
        organizationOwner = "facebook",
        permittedUsers = mutableListOf(
          PermittedUserData(
            "vaclav.novak@fake.com",
            Permission.ProjectPermissionType.TRANSLATE

          )
        ),
        keyData = mapOf(Pair("test", mapOf(Pair("en", "This is test text!"))))
      ),
      ProjectDataItem(
        name = "Microsoft Word",
        organizationOwner = "microsoft",
        permittedUsers = mutableListOf(
          PermittedUserData(
            "vaclav.novak@fake.com",
            Permission.ProjectPermissionType.MANAGE
          )
        ),
        keyData = mapOf(Pair("test", mapOf(Pair("en", "This is test text!"))))
      ),
      ProjectDataItem(
        name = "Microsoft Excel",
        organizationOwner = "microsoft",
        permittedUsers = mutableListOf(
          PermittedUserData(
            "vaclav.novak@fake.com",
            Permission.ProjectPermissionType.EDIT
          )
        ),
        keyData = mapOf(Pair("test", mapOf(Pair("en", "This is test text!"))))
      ),
      ProjectDataItem(
        name = "Microsoft Powerpoint",
        organizationOwner = "microsoft",
        permittedUsers = mutableListOf(
          PermittedUserData(
            "vaclav.novak@fake.com",
            Permission.ProjectPermissionType.TRANSLATE
          )
        ),
        keyData = mapOf(Pair("test", mapOf(Pair("en", "This is test text!"))))
      ),
      ProjectDataItem(
        name = "Microsoft Frontpage",
        organizationOwner = "microsoft",
        permittedUsers = mutableListOf(
          PermittedUserData(
            "vaclav.novak@fake.com",
            Permission.ProjectPermissionType.VIEW
          )
        ),
        keyData = mapOf(Pair("test", mapOf(Pair("en", "This is test text!"))))
      ),
      ProjectDataItem(
        name = "Vaclav's cool project",
        userOwner = "vaclav.novak@fake.com",
        permittedUsers = mutableListOf(
          PermittedUserData(
            "cukrberg@facebook.com",
            Permission.ProjectPermissionType.VIEW
          )
        ),
        keyData = mapOf(Pair("test", mapOf(Pair("en", "This is test text!"))))
      ),
      ProjectDataItem(
        name = "Vaclav's funny project",
        userOwner = "vaclav.novak@fake.com",
        permittedUsers = mutableListOf(
          PermittedUserData(
            "cukrberg@facebook.com",
            Permission.ProjectPermissionType.MANAGE
          )
        ),
        keyData = mapOf(Pair("test", mapOf(Pair("en", "This is test text!"))))
      )
    )

    init {
      (1..20).forEach { number ->
        val email = "owner@zzzcool$number.com"
        users.add(UserData(email))
        projects.find { item -> item.name == "Microsoft Word" }!!.permittedUsers.add(
          PermittedUserData(email, Permission.ProjectPermissionType.EDIT)
        )
      }
    }
  }
}
