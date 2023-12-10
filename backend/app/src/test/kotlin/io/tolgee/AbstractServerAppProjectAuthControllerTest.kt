package io.tolgee

import io.tolgee.testing.TestOverridesConfiguration
import io.tolgee.testing.TestsConfiguration
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  classes = [
    Application::class,
    TestsConfiguration::class,
    TestOverridesConfiguration::class,
    ServerAppTestOverridesConfiguration::class
  ]
)
abstract class AbstractServerAppProjectAuthControllerTest(
  projectUrlPrefix: String = "/api/project/"
) : ProjectAuthControllerTest(projectUrlPrefix)
