package io.tolgee.model

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.annotation.ActivityReturnsExistence
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.events.OnLanguagePrePersist
import io.tolgee.model.mtServiceConfig.MtServiceConfig
import io.tolgee.model.task.Task
import io.tolgee.model.translation.Translation
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.context.ApplicationEventPublisher
import java.util.Date

@Entity
@EntityListeners(Language.Companion.LanguageListeners::class)
@Table(
  indexes = [
    Index(
      columnList = "tag",
      name = "index_tag",
    ),
    Index(
      columnList = "tag, project_id",
      name = "index_tag_project",
    ),
    Index(
      columnList = "project_id",
      name = "index_project_id",
    ),
  ],
)
@ActivityLoggedEntity
@ActivityReturnsExistence
class Language :
  StandardAuditModel(),
  ILanguage,
  SoftDeletable {
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "language", orphanRemoval = true)
  var translations: MutableList<Translation> = mutableListOf()

  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var project: Project

  @Column(nullable = false)
  @field:NotEmpty
  @ActivityLoggedProp
  @ActivityDescribingProp
  override var tag: String = ""

  @ActivityLoggedProp
  @ActivityDescribingProp
  @field:NotBlank
  override var name: String = ""

  @ActivityLoggedProp
  override var originalName: String? = null

  @field:Size(max = 20)
  @Column(length = 20)
  @ActivityLoggedProp
  @ActivityDescribingProp
  override var flagEmoji: String? = null

  @OneToOne(mappedBy = "targetLanguage", orphanRemoval = true, fetch = FetchType.LAZY)
  var mtServiceConfig: MtServiceConfig? = null

  @OneToOne(mappedBy = "targetLanguage", orphanRemoval = true, fetch = FetchType.LAZY)
  var autoTranslationConfig: AutoTranslationConfig? = null

  @OneToOne(mappedBy = "language", orphanRemoval = true, fetch = FetchType.LAZY)
  var stats: LanguageStats? = null

  @OneToMany(mappedBy = "language", orphanRemoval = true, fetch = FetchType.LAZY)
  var tasks: MutableList<Task> = mutableListOf()

  @ActivityLoggedProp
  @Column(columnDefinition = "text")
  override var aiTranslatorPromptDescription: String? = null

  @ActivityLoggedProp
  override var deletedAt: Date? = null

  fun updateByDTO(dto: LanguageRequest) {
    name = dto.name
    tag = dto.tag
    originalName = dto.originalName
    flagEmoji = dto.flagEmoji
  }

  override fun toString(): String {
    return "Language(tag=$tag, name=$name, originalName=$originalName)"
  }

  companion object {
    @JvmStatic
    fun fromRequestDTO(dto: LanguageRequest): Language {
      val language = Language()
      language.name = dto.name
      language.tag = dto.tag
      language.originalName = dto.originalName
      language.flagEmoji = dto.flagEmoji
      return language
    }

    @Configurable
    class LanguageListeners {
      @Autowired
      lateinit var eventPublisherProvider: ObjectFactory<ApplicationEventPublisher>

      @PrePersist
      fun prePersist(language: Language) {
        eventPublisherProvider.`object`.publishEvent(OnLanguagePrePersist(source = this, language))
      }
    }
  }
}
