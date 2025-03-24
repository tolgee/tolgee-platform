package io.tolgee.dtos.request.prompt

class PromptVariable(
  val name: String,
  var value: String,
  var lazyValue: (() -> String)? = null,
)
