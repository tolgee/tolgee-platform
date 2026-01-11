package io.tolgee.dtos

object ExportParamsDocs {
  const val LANGUAGES_DESCRIPTION = """Languages to be contained in export.
                
If null, all languages are exported"""

  const val LANGUAGES_EXAMPLE = "en"

  const val FORMAT_DESCRIPTION = """Format to export to"""

  const val STRUCTURE_DELIMITER_DESCRIPTION = """Delimiter to structure file content. 

e.g. For key "home.header.title" would result in {"home": {"header": "title": {"Hello"}}} structure.

When null, resulting file won't be structured. Works only for generic structured formats (e.g. JSON, YAML), 
specific formats like `YAML_RUBY` don't honor this parameter."""

  const val SUPPORT_ARRAYS_DESCRIPTION = """If true, for structured formats (like JSON) arrays are supported. 

e.g. Key hello[0] will be exported as {"hello": ["..."]}"""

  const val FILTER_KEY_ID_DESCRIPTION = """Filter key IDs to be contained in export"""

  const val FILTER_KEY_ID_NOT_DESCRIPTION = """Filter key IDs not to be contained in export"""

  const val FILTER_TAG_DESCRIPTION = """Filter keys tagged by.

This filter works the same as `filterTagIn` but in this cases it accepts single tag only."""

  const val FILTER_TAG_IN_DESCRIPTION = """Filter keys tagged by one of provided tags"""

  const val FILTER_TAG_NOT_IN_DESCRIPTION = """Filter keys not tagged by one of provided tags"""

  const val FILTER_KEY_PREFIX_DESCRIPTION = """Filter keys with prefix"""

  const val FILTER_STATE_DESCRIPTION =
    """Filter translations with state. By default, all states except untranslated is exported."""

  const val FILTER_NAMESPACE_DESCRIPTION =
    "Filter translations with namespace. " +
      "By default, all namespaces everything are exported. To export default namespace, use empty string."

  const val MESSAGE_FORMAT_DESCRIPTION = """Message format to be used for export.
      
e.g. PHP_PO: Hello %s, ICU: Hello {name}. 

This property is honored only for generic formats like JSON or YAML. 
For specific formats like `YAML_RUBY` it's ignored."""

  const val ZIP_DESCRIPTION = """If false, it doesn't return zip of files, but it returns single file.
      
This is possible only when single language is exported. Otherwise it returns "400 - Bad Request" response."""

  const val FILE_STRUCTURE_TEMPLATE_DESCRIPTION =
    """This is a template that defines the structure of the resulting .zip file content.

The template is a string that can contain the following placeholders: {namespace}, {languageTag}, 
{androidLanguageTag}, {snakeLanguageTag}, {extension}. 

For example, when exporting to JSON with the template `{namespace}/{languageTag}.{extension}`, 
the English translations of the `home` namespace will be stored in `home/en.json`.

The `{snakeLanguageTag}` placeholder is the same as `{languageTag}` but in snake case. (e.g., en_US).

The Android specific `{androidLanguageTag}` placeholder is the same as `{languageTag}` 
but in Android format. (e.g., en-rUS)
"""

  const val HTML_ESCAPE_DESCRIPTION =
    """If true, HTML tags are escaped in the exported file. (Supported in the XLIFF format only).

e.g. Key <b>hello</b> will be exported as &lt;b&gt;hello&lt;/b&gt;"""
}
