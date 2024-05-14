package io.tolgee.dtos.request

class ComplexTagKeysRequest(
  val filterKeys: List<KeyId>?,
  val filterTag: List<String>?,
  val filterTagNot: List<String>?,
  val tagFiltered: List<String>?,
  val untagFiltered: List<String>?,
  val tagOther: List<String>?,
  val untagOther: List<String>?,
)
