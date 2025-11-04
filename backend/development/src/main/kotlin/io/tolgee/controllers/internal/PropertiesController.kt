package io.tolgee.controllers.internal

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.SetPropertyDto
import io.tolgee.facade.InternalPropertiesSetterFacade
import jakarta.validation.Valid
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody

@InternalController(["internal/properties"])
class PropertiesController(
  private val tolgeeProperties: TolgeeProperties,
  private val internalPropertiesSetterFacade: InternalPropertiesSetterFacade,
) {
  @PutMapping(value = ["/set"])
  @Transactional
  fun setProperty(
    @RequestBody @Valid
    setPropertyDto: SetPropertyDto,
  ) {
    internalPropertiesSetterFacade.setProperty(tolgeeProperties, setPropertyDto)
  }
}
