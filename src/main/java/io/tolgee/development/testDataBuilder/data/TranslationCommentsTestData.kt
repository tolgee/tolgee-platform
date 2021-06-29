package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.DataBuilders
import io.tolgee.development.testDataBuilder.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment

class TranslationCommentsTestData {
    private lateinit var firstComment: TranslationComment
    private lateinit var secondComment: TranslationComment
    var project: Project
    lateinit var englishLanguage: Language
    var user: UserAccount
    lateinit var aKey: Key
    lateinit var projectBuilder: DataBuilders.ProjectBuilder
    lateinit var translation: Translation

    val root: TestDataBuilder = TestDataBuilder().apply {
        user = addUserAccount {
            self {
                username = "franta"
            }
        }.self
        project = addProject {
            self {
                name = "Franta's project"
                userOwner = user
            }

            addPermission {
                self {
                    project = this@addProject.self
                    user = this@TranslationCommentsTestData.user
                    type = Permission.ProjectPermissionType.MANAGE
                }
            }

            englishLanguage = addLanguage {
                self {
                    name = "English"
                    tag = "en"
                    originalName = "English"
                }
            }.self

            aKey = addKey {
                self.name = "A key"
                translation = addTranslation {
                    self {
                        key = this@addKey.self
                        language = englishLanguage
                        text = "Z translation"
                        state = TranslationState.REVIEWED
                    }
                    firstComment = addComment {
                        self {
                            text = "First comment"
                        }
                    }.self

                    secondComment = addComment {
                        self {
                            text = "Second comment"
                        }
                    }.self
                }.self
            }.self
            projectBuilder = this
        }.self
    }
}
