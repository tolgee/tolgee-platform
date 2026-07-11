package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.DbPopulatorReal
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewRepeatableTransaction
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/organizations"])
class OrganizationE2eDataController(
  private val organizationService: OrganizationService,
  private val userAccountService: UserAccountService,
  private val dbPopulatorReal: DbPopulatorReal,
  private val organizationRoleService: OrganizationRoleService,
  private val transactionManager: PlatformTransactionManager,
) : Logging {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun createOrganizations(): Map<String, Map<String, String>> {
    val organizations =
      data.map {
        organizationService.create(
          it.dto,
          this.dbPopulatorReal.createUserIfNotExists(it.owner.email, null, it.owner.name),
        )
      }

    data.forEach { dataItem ->
      organizationService.findAllByName(dataItem.dto.name).forEach { organization ->
        dataItem.members.forEach { memberUserName ->
          val user = userAccountService.findActive(memberUserName) ?: throw NotFoundException()
          organizationRoleService.grantMemberRoleToUser(user, organization)
        }

        dataItem.managedMembers.forEach { memberUserName ->
          val user = userAccountService.findActive(memberUserName) ?: throw NotFoundException()
          organizationRoleService.grantMemberRoleToUser(user, organization)
          organizationRoleService.setManaged(user, organization, true)
        }

        dataItem.otherOwners.forEach { memberUserName ->
          val user = userAccountService.findActive(memberUserName) ?: throw NotFoundException()
          organizationRoleService.grantOwnerRoleToUser(user, organization)
        }
      }
    }
    return organizations.associate { it.name to mapOf("slug" to it.slug) }
  }

  @GetMapping(value = ["/clean"])
  fun cleanupOrganizations() {
    traceLogMeasureTime("cleanupOrganizations") {
      executeInNewRepeatableTransaction(
        transactionManager,
        isolationLevel = TransactionDefinition.ISOLATION_SERIALIZABLE,
      ) {
        traceLogMeasureTime("delete what-a-nice-organization") {
          organizationService.findAllByName("What a nice organization").forEach {
            organizationService.delete(it)
          }
        }
        data.forEach {
          traceLogMeasureTime("delete organization ${it.dto.slug}") {
            val orgs =
              traceLogMeasureTime("find organization") {
                organizationService.findAllByName(it.dto.name)
              }
            orgs.forEach { organization ->
              traceLogMeasureTime("delete organization") {
                organizationService.delete(organization)
              }
            }
          }
        }
        traceLogMeasureTime("delete users") {
          val owners =
            data.mapNotNull {
              if (it.owner.name == "admin") return@mapNotNull null
              it.owner.email
            }
          userAccountService.deleteByUserNames(owners)
        }
      }
    }
  }

  companion object {
    data class UserData(
      val email: String,
      val name: String = email,
    )

    data class OrganizationDataItem(
      val dto: OrganizationDto,
      val owner: UserData,
      val otherOwners: MutableList<String> = mutableListOf(),
      val members: MutableList<String> = mutableListOf(),
      val managedMembers: MutableList<String> = mutableListOf(),
    )

    val data =
      mutableListOf(
        OrganizationDataItem(
          dto =
            OrganizationDto(
              name = "Google",
              description = "An organization made by google company",
            ),
          owner = UserData("admin"),
        ),
        OrganizationDataItem(
          dto =
            OrganizationDto(
              name = "Netsuite",
              description = "A system for everything",
            ),
          owner = UserData("evan@netsuite.com", "Evan Goldberg"),
        ),
        OrganizationDataItem(
          dto =
            OrganizationDto(
              name = "Microsoft",
              description = "A first software company ever or something like that.",
            ),
          owner = UserData("gates@microsoft.com", "Bill Gates"),
          members = mutableListOf("admin"),
        ),
        OrganizationDataItem(
          dto =
            OrganizationDto(
              name = "Tolgee",
              description = "This is us",
            ),
          owner = UserData("admin"),
          otherOwners = mutableListOf("evan@netsuite.com"),
          members = mutableListOf("gates@microsoft.com", "cukrberg@facebook.com"),
          managedMembers = mutableListOf("LonelyDev@tolgee.io"),
        ),
        OrganizationDataItem(
          dto =
            OrganizationDto(
              name = "What a nice organization",
              description = "We are very nice",
            ),
          owner = UserData("LonelyDev@tolgee.io", "Lonely Developer"),
        ),
        OrganizationDataItem(
          dto =
            OrganizationDto(
              name = "Facebook",
              description =
                """
                            |This is an organization providing a great service to everyone for free. 
                            |They also develop amazing things like react and other open source stuff.
                            |However, they sell our data to companies.
                """.trimMargin(),
            ),
          owner = UserData("cukrberg@facebook.com", "Mark Cukrberg"),
          otherOwners = mutableListOf("admin"),
        ),
        OrganizationDataItem(
          dto =
            OrganizationDto(
              name = "Unknown company",
              description = "We are very unknown.",
            ),
          owner = UserData("admin"),
        ),
        OrganizationDataItem(
          dto =
            OrganizationDto(
              name = "Techfides solutions s.r.o",
              description = "Lets develop the future",
            ),
          owner = UserData("admin"),
        ),
      )

    init {
      (1..20).forEach { number ->
        val email = "owner@zzzcool$number.com"
        data.add(
          OrganizationDataItem(
            dto =
              OrganizationDto(
                name = "ZZZ Cool company $number",
                description = "We are Z Cool company $number. What a nice day!",
              ),
            otherOwners = mutableListOf("admin"),
            owner = UserData(email),
          ),
        )
        data.find { item -> item.dto.name == "Facebook" }!!.otherOwners.add(email)
      }
    }
  }
}
