/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.dtos.request.GenerateAddressPathDto
import io.tolgee.service.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.validation.Valid


@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/address-part", "/api/address-part"])
open class AddressPartController(
        private val organizationService: OrganizationService,
        private val repositoryService: RepositoryService,
) {

    @GetMapping("/validate-organization/{addressPart}")
    @Operation(summary = "Validate organization address part")
    open fun validateOrganizationAddressPart(
            @PathVariable("addressPart") addressPart: String
    ): Boolean {
        return organizationService.validateAddressPartUniqueness(addressPart)
    }


    @GetMapping("/validate-repository/{addressPart}")
    @Operation(summary = "Validate repository address part")
    open fun validateRepositoryAddressPart(
            @PathVariable("addressPart") addressPart: String
    ): Boolean {
        return repositoryService.validateAddressPartUniqueness(addressPart)
    }

    @PostMapping("/generate-organization", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Generate organization address part")
    open fun generateOrganizationAddressPart(
            @RequestBody @Valid dto: GenerateAddressPathDto
    ): String {
        return """"${organizationService.generateAddressPart(dto.name!!, dto.oldAddressPart)}""""
    }


    @PostMapping("/generate-repository", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Generate repository address part")
    open fun generateRepositoryAddressPart(
            @RequestBody @Valid dto: GenerateAddressPathDto
    ): String {
        return """"${repositoryService.generateAddressPart(dto.name!!, dto.oldAddressPart)}""""
    }
}
