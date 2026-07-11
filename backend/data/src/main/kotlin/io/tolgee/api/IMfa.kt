package io.tolgee.api

interface IMfa {
  val totpKey: ByteArray?
}

val IMfa.isMfaEnabled: Boolean
  get() = this.totpKey?.isNotEmpty() ?: false
