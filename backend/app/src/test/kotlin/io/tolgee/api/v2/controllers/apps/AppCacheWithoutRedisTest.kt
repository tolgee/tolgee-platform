package io.tolgee.api.v2.controllers.apps

import io.tolgee.testing.ContextRecreatingTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.annotation.DirtiesContext

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "tolgee.internal.fake-mt-providers=false",
  ],
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AppCacheWithoutRedisTest : AbstractAppCacheTest()
