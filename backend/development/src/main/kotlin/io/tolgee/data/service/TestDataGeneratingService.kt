package io.tolgee.data.service

import io.tolgee.data.StandardTestDataResult
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TestDataGeneratingService(
  private val testDataService: TestDataService,
) {
  @Transactional
  fun generate(
    testData: TestDataBuilder,
    afterTestDataStored: (TestDataBuilder) -> Unit = {},
  ): StandardTestDataResult {
    testDataService.saveTestData(testData)
    afterTestDataStored(testData)
    return getStandardResult(testData)
  }

  fun getStandardResult(data: TestDataBuilder): StandardTestDataResult {
    return StandardTestDataResult(
      projects =
        data.data.projects.map {
          StandardTestDataResult.ProjectModel(name = it.self.name, id = it.self.id)
        },
      users =
        data.data.userAccounts.map {
          StandardTestDataResult.UserModel(name = it.self.name, username = it.self.username, id = it.self.id)
        },
      organizations =
        data.data.organizations.map {
          StandardTestDataResult.OrganizationModel(
            id = it.self.id,
            name = it.self.name,
            slug = it.self.slug,
            glossaries =
              it.data.glossaries.map {
                StandardTestDataResult.GlossaryModel(id = it.self.id, name = it.self.name)
              },
          )
        },
      invitations =
        data.data.invitations.map {
          StandardTestDataResult.InvitationModel(
            projectId =
              it.self.permission
                ?.project
                ?.id,
            organizationId =
              it.self.permission
                ?.organization
                ?.id,
            code = it.self.code,
          )
        },
    )
  }
}
