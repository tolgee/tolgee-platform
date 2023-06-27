package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class BatchOperationManagementControllerTest : ProjectAuthControllerTest("/v2/projects/")
