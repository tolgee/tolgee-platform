package io.tolgee.development.testDataBuilder.data

class ExportTagAndNamespaceFilterTestData : BaseTestData("export_tag_filter_user", "Export tag filter project") {
  init {
    projectBuilder.self.useNamespaces = true
    projectBuilder.apply {
      addTaggedKey(null, "default-included", "included")
      addTaggedKey(null, "default-included-excluded", "included", "excluded")
      addTaggedKey(null, "default-feature", "feature-login")
      addTaggedKey(null, "default-untagged")

      addTaggedKey("ns-1", "ns1-included", "included")
      addTaggedKey("ns-1", "ns1-included-excluded", "included", "excluded")
      addTaggedKey("ns-1", "ns1-feature", "feature-signup")
      addTaggedKey("ns-1", "ns1-untagged")

      addTaggedKey("ns-2", "ns2-included", "included")
      addTaggedKey("ns-2", "ns2-excluded", "excluded")
      addTaggedKey("ns-2", "ns2-untagged")
    }
  }

  private fun addTaggedKey(
    namespace: String?,
    keyName: String,
    vararg tags: String,
  ) {
    projectBuilder.addKey(namespace, keyName) {
      addTranslation("en", "$keyName text")
      tags.forEach { addTag(it) }
    }
  }
}
