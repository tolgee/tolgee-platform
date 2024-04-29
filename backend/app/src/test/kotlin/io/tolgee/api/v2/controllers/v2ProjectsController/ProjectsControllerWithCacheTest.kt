package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.testing.ContextRecreatingTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
@AutoConfigureMockMvc
class ProjectsControllerWithCacheTest : ProjectsControllerTest()
