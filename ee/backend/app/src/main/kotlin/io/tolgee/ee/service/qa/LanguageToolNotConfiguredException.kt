package io.tolgee.ee.service.qa

class LanguageToolNotConfiguredException :
  RuntimeException(
    "LanguageTool is not configured. Set tolgee.language-tool.url to enable spelling and grammar checks.",
  )
