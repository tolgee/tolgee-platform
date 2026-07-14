package io.tolgee.activity.groups.data

interface ActivityEntityView {
  val activityRevisionId: Long
  val entityId: Long
  val entityClass: String
  val additionalDescription: Map<String, Any?>?
}

inline fun <reified T> ActivityEntityView.getFieldFromViewNullable(name: String): T? {
  return when (this) {
    is DescribingEntityView ->
      this.data[name] as? T

    is ModifiedEntityView ->
      this.describingData[name] as? T ?: this.modifications[name]?.let { it.new as? T }

    else -> throw IllegalArgumentException("Unknown entity view type")
  }
}

inline fun <reified T> ActivityEntityView.getFieldFromView(name: String): T {
  return this.getFieldFromViewNullable(name)
    ?: throw IllegalArgumentException("Field $name not found in describing data")
}

inline fun <reified T> ActivityEntityView.getAdditionalDescriptionFieldNullable(
  fieldName: String,
  propertyName: String,
): T? {
  val field = this.additionalDescription?.get(fieldName) as? Map<String, Any?>
  return field?.get(propertyName) as? T
}
