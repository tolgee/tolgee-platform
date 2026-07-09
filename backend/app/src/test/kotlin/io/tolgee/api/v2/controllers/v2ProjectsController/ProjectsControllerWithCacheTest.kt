package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.testing.ContextRecreatingTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
@AutoConfigureMockMvc
class ProjectsControllerWithCacheTest : ProjectsControllerTest()
