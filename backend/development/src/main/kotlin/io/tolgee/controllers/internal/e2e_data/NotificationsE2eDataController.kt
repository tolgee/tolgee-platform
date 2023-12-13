/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.controllers.internal.e2e_data

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.NotificationsTestData
import io.tolgee.repository.UserAccountRepository
import io.tolgee.service.security.UserAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/notifications"])
class NotificationsE2eDataController : AbstractE2eDataController() {
  @Autowired
  lateinit var userAccountRepository: UserAccountRepository

  @Autowired
  lateinit var userAccountService: UserAccountService

  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData() {
    userAccountService.findActive("admin")?.let {
      userAccountService.delete(it)
      userAccountRepository.delete(it)
    }

    testDataService.saveTestData(testData)
  }

  override val testData: TestDataBuilder
    get() = NotificationsTestData().root
}
