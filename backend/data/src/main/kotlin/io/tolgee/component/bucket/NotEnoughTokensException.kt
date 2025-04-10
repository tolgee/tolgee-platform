package io.tolgee.component.bucket

class NotEnoughTokensException(
  val refillAt: Long,
) : RuntimeException("Not enough credits in the bucket")
