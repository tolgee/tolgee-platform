package io.tolgee.api.v2.hateoas.organization

import org.springframework.hateoas.RepresentationModel

data class OrganizationModel(val name: String) : RepresentationModel<OrganizationModel>()
