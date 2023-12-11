package io.tolgee

@ServerAppTestContextConfiguration
abstract class AbstractServerAppProjectAuthControllerTest(
  projectUrlPrefix: String = "/api/project/"
) : ProjectAuthControllerTest(projectUrlPrefix)
