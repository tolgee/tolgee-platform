package io.tolgee.exceptions

import io.tolgee.model.import.issues.issueTypes.FileIssueType

class FileIssueException(
        val type: FileIssueType,
        val params: List<Any?>? = null
) : Throwable()
