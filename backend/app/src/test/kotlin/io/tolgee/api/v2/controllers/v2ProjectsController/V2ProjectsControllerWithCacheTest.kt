package io.tolgee.api.v2.controllers.v2ProjectsController

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ]
)
@AutoConfigureMockMvc
class V2ProjectsControllerWithCacheTest : V2ProjectsControllerTest()
