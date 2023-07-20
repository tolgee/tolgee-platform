package io.tolgee.service

import io.tolgee.component.email.InvitationEmailSender
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.misc.CreateInvitationParams
import io.tolgee.dtos.misc.CreateOrganizationInvitationParams
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Invitation
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.InvitationRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.security.PermissionService
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class InvitationService @Autowired constructor(
  private val invitationRepository: InvitationRepository,
  private val authenticationFacade: AuthenticationFacade,
  private val organizationRoleService: OrganizationRoleService,
  private val permissionService: PermissionService,
  private val invitationEmailSender: InvitationEmailSender,
  private val businessEventPublisher: BusinessEventPublisher
) {
  @Transactional
  fun create(params: CreateProjectInvitationParams): Invitation {
    return create(params) { invitation ->
      permissionService.createForInvitation(
        invitation = invitation,
        params
      )
    }
  }

  /**
   * Creates invitations for project
   *
   * Enables to provide custom function returning permission,
   * to be able to create special permission when user provided scopes
   */
  @Transactional
  fun create(
    params: CreateProjectInvitationParams,
    setPermissionFn: (invitation: Invitation) -> Permission
  ): Invitation {
    checkEmailNotAlreadyInvited(params)
    val invitation = getInvitationInstance(params)
    invitation.permission = setPermissionFn(invitation)
    invitationEmailSender.sendInvitation(invitation)
    return invitationRepository.save(invitation)
  }

  @Transactional
  fun create(params: CreateOrganizationInvitationParams): Invitation {
    checkEmailNotAlreadyInvited(params)

    val invitation = getInvitationInstance(params)

    invitation.organizationRole = organizationRoleService.createForInvitation(
      invitation = invitation,
      type = params.type,
      organization = params.organization
    )
    invitationRepository.save(invitation)

    invitationEmailSender.sendInvitation(invitation)

    return invitation
  }

  private fun getInvitationInstance(params: CreateInvitationParams): Invitation {
    val code = RandomStringUtils.randomAlphabetic(50)
    val invitation = Invitation(code = code)
    invitation.email = params.email
    invitation.name = params.name
    return invitation
  }

  @Transactional
  fun removeExpired() {
    invitationRepository.deleteAllByCreatedAtLessThan(Date.from(Instant.now().minus(Duration.ofDays(30))))
  }

  @Transactional
  fun accept(code: String?) {
    this.accept(code, authenticationFacade.userAccountEntity)
  }

  @Transactional
  fun accept(code: String?, userAccount: UserAccount) {
    val invitation = getInvitation(code)
    val permission = invitation.permission
    val organizationRole = invitation.organizationRole

    validateProjectXorOrganization(permission, organizationRole)

    acceptProjectInvitation(permission, userAccount)
    acceptOrganizationInvitation(organizationRole, userAccount)

    // avoid cascade delete
    invitation.permission = null
    invitation.organizationRole = null
    invitationRepository.delete(invitation)
  }

  private fun acceptProjectInvitation(permission: Permission?, userAccount: UserAccount) {
    permission?.let {
      acceptProjectInvitation(permission, userAccount)
      businessEventPublisher.publish(
        OnBusinessEventToCaptureEvent(
          eventName = "PROJECT_INVITATION_ACCEPTED",
          userAccountId = userAccount.id,
          userAccountDto = UserAccountDto.fromEntity(userAccount)
        )
      )
    }
  }

  private fun acceptOrganizationInvitation(organizationRole: OrganizationRole?, userAccount: UserAccount) {
    organizationRole?.let {
      acceptOrganizationInvitation(userAccount, organizationRole)
      businessEventPublisher.publish(
        OnBusinessEventToCaptureEvent(
          eventName = "ORGANIZATION_INVITATION_ACCEPTED",
          userAccountId = userAccount.id,
          userAccountDto = UserAccountDto.fromEntity(userAccount),
          organizationId = it.organization?.id,
          organizationName = it.organization?.name
        )
      )
    }
  }

  private fun validateProjectXorOrganization(
    permission: Permission?,
    organizationRole: OrganizationRole?
  ) {
    if (!(permission == null).xor(organizationRole == null)) {
      throw IllegalStateException("Exactly of permission and organizationRole may be set")
    }
  }

  private fun acceptOrganizationInvitation(
    userAccount: UserAccount,
    organizationRole: OrganizationRole
  ) {
    if (organizationRoleService.isUserMemberOrOwner(userAccount.id, organizationRole.organization!!.id)) {
      throw BadRequestException(Message.USER_ALREADY_HAS_ROLE)
    }
    organizationRoleService.acceptInvitation(organizationRole, userAccount)
  }

  private fun acceptProjectInvitation(
    permission: Permission,
    userAccount: UserAccount
  ): Permission {
    if (permissionService.find(projectId = permission.project!!.id, userId = userAccount.id) != null) {
      throw BadRequestException(Message.USER_ALREADY_HAS_PERMISSIONS)
    }
    return permissionService.acceptInvitation(permission, userAccount)
  }

  @Transactional
  fun getInvitation(code: String?): Invitation {
    removeExpired()
    return invitationRepository.findOneByCode(code).orElseThrow {
      // this exception is important for sign up service! Do not remove!!
      BadRequestException(Message.INVITATION_CODE_DOES_NOT_EXIST_OR_EXPIRED)
    }!!
  }

  fun findById(id: Long): Optional<Invitation> {
    @Suppress("UNCHECKED_CAST")
    return invitationRepository.findById(id) as Optional<Invitation>
  }

  fun getForProject(project: Project): Set<Invitation> {
    return invitationRepository.findAllByPermissionProjectOrderByCreatedAt(project)
  }

  @Transactional
  fun delete(invitation: Invitation) {
    invitation.permission?.let {
      permissionService.delete(it)
    }
    if (invitation.organizationRole != null) {
      organizationRoleService
    }
    invitationRepository.delete(invitation)
  }

  fun getForOrganization(organization: Organization): List<Invitation> {
    return invitationRepository.getAllByOrganizationRoleOrganizationOrderByCreatedAt(organization)
  }

  private fun checkEmailNotAlreadyInvited(params: CreateProjectInvitationParams) {
    val email = params.email
    if (!email.isNullOrEmpty() && userOrInvitationWithEmailExists(params.email, params.project)) {
      throw BadRequestException(Message.EMAIL_ALREADY_INVITED_OR_MEMBER)
    }
  }

  private fun checkEmailNotAlreadyInvited(params: CreateOrganizationInvitationParams) {
    val email = params.email
    if (!email.isNullOrEmpty() && userOrInvitationWithEmailExists(params.email, params.organization)) {
      throw BadRequestException(Message.EMAIL_ALREADY_INVITED_OR_MEMBER)
    }
  }

  fun userOrInvitationWithEmailExists(email: String, project: Project): Boolean {
    return invitationRepository.countByUserOrInvitationWithEmailAndProject(email, project) > 0
  }

  fun userOrInvitationWithEmailExists(email: String, organization: Organization): Boolean {
    return invitationRepository.countByUserOrInvitationWithEmailAndOrganization(email, organization) > 0
  }
}
