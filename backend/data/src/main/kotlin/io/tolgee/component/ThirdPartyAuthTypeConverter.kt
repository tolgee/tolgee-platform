package io.tolgee.component

import io.tolgee.model.enums.ThirdPartyAuthType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter()
class ThirdPartyAuthTypeConverter : AttributeConverter<ThirdPartyAuthType, String> {
  override fun convertToDatabaseColumn(attribute: ThirdPartyAuthType?): String? = attribute?.code()

  override fun convertToEntityAttribute(dbData: String?): ThirdPartyAuthType? = dbData?.uppercase()?.let { ThirdPartyAuthType.valueOf(it) }
}
