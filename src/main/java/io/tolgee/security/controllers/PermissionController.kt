package io.tolgee.security.controllers

import io.tolgee.constants.Message
import io.tolgee.dtos.request.PermissionEditDto
import io.tolgee.dtos.response.PermissionDTO
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.PermissionService
import io.tolgee.service.RepositoryService
import io.tolgee.service.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.stream.Collectors
import javax.validation.Valid

@RestController
@RequestMapping("/api/permission")
class PermissionController @Autowired constructor(private val securityService: SecurityService,
                                                  private val repositoryService: RepositoryService,
                                                  private val permissionService: PermissionService,
                                                  private val authenticationFacade: AuthenticationFacade) {
    @GetMapping("/list/{repositoryId}")
    fun getRepositoryPermissions(@PathVariable("repositoryId") id: Long?): Set<PermissionDTO> {
        val repository = repositoryService.get(id!!).orElseThrow({ NotFoundException() })
        securityService.checkRepositoryPermission(id, Permission.RepositoryPermissionType.MANAGE)
        return permissionService.getAllOfRepository(repository).stream()
                .map { entity: Permission? -> PermissionDTO.fromEntity(entity) }
                .collect(Collectors.toCollection { LinkedHashSet() })
    }

    @DeleteMapping("/{permissionId}")
    fun deletePermission(@PathVariable("permissionId") id: Long?) {
        val permission = permissionService.findById(id!!) ?: throw NotFoundException(Message.PERMISSION_NOT_FOUND)
        securityService.checkRepositoryPermission(permission.repository!!.id, Permission.RepositoryPermissionType.MANAGE)
        if (permission.user!!.id == authenticationFacade.userAccount.id) {
            throw BadRequestException(Message.CAN_NOT_REVOKE_OWN_PERMISSIONS)
        }
        permissionService.delete(permission)
    }

    @PostMapping("edit")
    fun editPermission(@RequestBody @Valid dto: PermissionEditDto?) {
        val permission: Permission = permissionService.findById(dto!!.permissionId)
                ?: throw NotFoundException(Message.PERMISSION_NOT_FOUND)
        securityService.checkRepositoryPermission(permission.repository!!.id, Permission.RepositoryPermissionType.MANAGE)
        permissionService.editPermission(permission, dto.type)
    }
}
