package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.controllers.IController
import io.tolgee.model.enums.Scope
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.SortDefault
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.tags.Tag as OpenApiTag

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/public/scope-info"
  ]
)
@OpenApiTag(name = "Scope Info", description = "Returns info about permission scopes")
class ScopeInfoController : IController {

  @GetMapping(value = ["/hierarchy"])
  @Operation(summary = "Returns hierarchy of scopes")
  fun getHierarchy(
    search: String? = null
  ): Scope.HierarchyItem {
    return Scope.hierarchy
  }
}
