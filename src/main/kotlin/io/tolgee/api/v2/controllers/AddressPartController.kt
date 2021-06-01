/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.GenerateAddressPathDto
import io.tolgee.service.OrganizationService
import io.tolgee.service.ProjectService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.validation.Valid


@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/address-part", "/api/address-part"])
@Tag(name = "Address Part generation")
open class AddressPartController(
        private val organizationService: OrganizationService,
        private val projectService: ProjectService,
) {

    @GetMapping("/validate-organization/{addressPart}")
    @Operation(summary = "Validate organization address part")
    open fun validateOrganizationAddressPart(
            @PathVariable("addressPart") addressPart: String
    ): Boolean {
        return organizationService.validateAddressPartUniqueness(addressPart)
    }


    @GetMapping("/validate-project/{addressPart}")
    @Operation(summary = "Validate project address part")
    open fun validateProjectAddressPart(
            @PathVariable("addressPart") addressPart: String
    ): Boolean {
        return projectService.validateAddressPartUniqueness(addressPart)
    }

    @PostMapping("/generate-organization", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Generate organization address part")
    open fun generateOrganizationAddressPart(
            @RequestBody @Valid dto: GenerateAddressPathDto
    ): String {
        return """"${organizationService.generateAddressPart(dto.name!!, dto.oldAddressPart)}""""
    }


    @PostMapping("/generate-project", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Generate project address part")
    open fun generateProjectAddressPart(
            @RequestBody @Valid dto: GenerateAddressPathDto
    ): String {
        return """"${projectService.generateAddressPart(dto.name!!, dto.oldAddressPart)}""""
    }
}
