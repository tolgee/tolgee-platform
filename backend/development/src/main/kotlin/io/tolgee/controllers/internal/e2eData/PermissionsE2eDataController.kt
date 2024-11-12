package io.tolgee.controllers.internal.e2eData

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.PermissionsTestData
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.annotation.RequestScope

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/permissions"])
@Transactional
@RequestScope
class PermissionsE2eDataController() : AbstractE2eDataController() {
  @GetMapping(value = ["/generate-with-user"])
  @Transactional
  fun generateWithPermissions(
    @RequestParam scopes: List<String>?,
    @RequestParam type: ProjectPermissionType?,
    @RequestParam viewLanguageTags: List<String>?,
    @RequestParam translateLanguageTags: List<String>?,
    @RequestParam stateChangeLanguageTags: List<String>?,
  ): StandardTestDataResult {
    val user =
      this.permissionsTestData.addUserWithPermissions(
        scopes = Scope.parse(scopes).toList(),
        type = type,
        viewLanguageTags = viewLanguageTags,
        translateLanguageTags = translateLanguageTags,
        stateChangeLanguageTags = stateChangeLanguageTags,
      )
    this.permissionsTestData.addTasks(
      mutableSetOf(user, permissionsTestData.serverAdmin.self),
    )
    return generate()
  }

  private val permissionsTestData by lazy {
    PermissionsTestData()
  }

  override fun cleanup(): Any? {
    // otherwise it won't clean the user created with "permissionsTestData.addUserWithPermissions"
    this.permissionsTestData.addUserWithPermissions(null, null, null, null, null)
    return super.cleanup()
  }

  override val testData: TestDataBuilder by lazy {
    permissionsTestData.root
  }
}
