package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import java.util.Date

class PatTestData {
  lateinit var user: UserAccount
  lateinit var user2: UserAccount
  lateinit var pat: Pat
  lateinit var expiredPat: Pat

  val root =
    TestDataBuilder().apply {
      addUserAccount {
        username = "peter@peter.com"
        name = "Peter Peter"
        role = UserAccount.Role.ADMIN
        user2 = this
      }

      addUserAccount {
        username = "user@user.com"
        name = "John User"
        role = UserAccount.Role.USER
        user = this
      }.build {
        addPat {
          description = "Expired PAT"
          expiresAt = Date(1661342685000)
          lastUsedAt = Date(1661342385000)
          expiredPat = this
        }
        addPat {
          description = "Yee2"
          pat = this
        }
        addPat {
          description = "Yeey3"
        }
      }
    }
}
