package io.tolgee.security

import io.tolgee.dtos.cacheable.OrganizationDto
import io.tolgee.model.Organization
import io.tolgee.service.organization.OrganizationService

open class OrganizationHolder(
  private val organizationService: OrganizationService,
) {
  open val organizationEntity: Organization by lazy {
    organizationService.get(organization.id)
  }

  private var _organization: OrganizationDto? = null
  open var organization: OrganizationDto
    set(value) {
      _organization = value
    }
    get() {
      return _organization ?: throw OrganizationNotSelectedException()
    }

  val organizationOrNull: OrganizationDto?
    get() = _organization
}
