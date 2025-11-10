package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.AliasFor
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping
@Transactional
@ConditionalOnProperty(
  value = ["tolgee.internal.controller-enabled"],
  havingValue = "true",
  matchIfMissing = false,
)
annotation class InternalController(
  @get:AliasFor(annotation = RequestMapping::class)
  val value: Array<String> = [],
)
