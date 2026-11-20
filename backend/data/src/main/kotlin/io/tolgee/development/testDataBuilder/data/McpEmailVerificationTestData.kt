package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Pat

class McpEmailVerificationTestData(
  verified: Boolean,
) : BaseTestData() {
  lateinit var pat: Pat

  init {
    userAccountBuilder.build {
      addPat {
        description = "MCP test PAT"
        pat = this
      }
      if (!verified) {
        setEmailVerification {
          code = "unverified-code"
        }
      }
    }
  }
}
