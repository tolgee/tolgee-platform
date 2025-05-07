package io.tolgee.model

import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.api.ISimpleProject
import io.tolgee.model.automations.Automation
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.key.Key
import io.tolgee.model.key.Namespace
import io.tolgee.model.mtServiceConfig.MtServiceConfig
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.webhook.WebhookConfig
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.annotations.ColumnDefault
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import java.util.*
import kotlin.jvm.Transient

@Entity
@Table(
  indexes = [
    Index(columnList = "user_owner_id"),
    Index(columnList = "organization_owner_id"),
  ],
)
@EntityListeners(Project.Companion.ProjectListener::class)
@ActivityLoggedEntity
class Project(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  override var id: Long = 0L,
  @field:NotBlank
  @field:Size(min = 3, max = 50)
  @ActivityLoggedProp
  override var name: String = "",
  @field:Size(min = 3, max = 2000)
  @ActivityLoggedProp
  @Column(length = 2000)
  override var description: String? = null,
  @field:Size(max = 2000)
  @Column(columnDefinition = "text")
  @ActivityLoggedProp
  var aiTranslatorPromptDescription: String? = null,
  @Column(name = "address_part")
  @ActivityLoggedProp
  @field:Size(min = 3, max = 60)
  @field:Pattern(regexp = "^[a-z0-9-]*[a-z]+[a-z0-9-]*$", message = "invalid_pattern")
  override var slug: String? = null,
) : AuditModel(), ModelWithAvatar, EntityWithId, SoftDeletable, ISimpleProject {
  @OrderBy("id")
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
  var languages: MutableSet<Language> = LinkedHashSet()

  @OneToMany(mappedBy = "project")
  var permissions: MutableSet<Permission> = LinkedHashSet()

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
  var keys: MutableList<Key> = mutableListOf()

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
  var apiKeys: MutableSet<ApiKey> = LinkedHashSet()

  @Suppress("SetterBackingFieldAssignment")
  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @Deprecated(message = "Project can be owned only by organization")
  var userOwner: UserAccount? = null

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  lateinit var organizationOwner: Organization

  @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
  @ActivityLoggedProp
  var baseLanguage: Language? = null

  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE], mappedBy = "project", orphanRemoval = true)
  var autoTranslationConfigs: MutableList<AutoTranslationConfig> = mutableListOf()

  @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "project")
  var mtServiceConfig: MutableList<MtServiceConfig> = mutableListOf()

  @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "project")
  var namespaces: MutableList<Namespace> = mutableListOf()

  @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
  @ActivityLoggedProp
  var defaultNamespace: Namespace? = null

  @ManyToMany(fetch = FetchType.LAZY, mappedBy = "assignedProjects")
  var glossaries: MutableSet<Glossary> = mutableSetOf()

  @ActivityLoggedProp
  override var avatarHash: String? = null

  @Transient
  @Column(insertable = false, updatable = false)
  override var disableActivityLogging = false

  @OneToMany(orphanRemoval = true, mappedBy = "project")
  var automations: MutableList<Automation> = mutableListOf()

  @OneToMany(orphanRemoval = true, mappedBy = "project")
  var contentDeliveryConfigs: MutableList<ContentDeliveryConfig> = mutableListOf()

  @OneToMany(orphanRemoval = true, mappedBy = "project")
  var contentStorages: MutableList<ContentStorage> = mutableListOf()

  @OneToMany(orphanRemoval = true, mappedBy = "project")
  var webhookConfigs: MutableList<WebhookConfig> = mutableListOf()

  @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "project")
  var slackConfigs: MutableList<SlackConfig> = mutableListOf()

  @ColumnDefault("true")
  override var icuPlaceholders: Boolean = true

  @ColumnDefault("false")
  @ActivityLoggedProp
  var useNamespaces: Boolean = false

  @ColumnDefault("0")
  var lastTaskNumber: Long = 0

  @ActivityLoggedProp
  override var deletedAt: Date? = null

  constructor(name: String, description: String? = null, slug: String?, organizationOwner: Organization) :
    this(id = 0L, name, description, slug) {
    this.organizationOwner = organizationOwner
  }

  fun findLanguageOptional(tag: String): Optional<Language> {
    return languages.stream().filter { l: Language -> (l.tag == tag) }.findFirst()
  }

  fun findLanguage(tag: String): Language? {
    return findLanguageOptional(tag).orElse(null)
  }

  /**
   * organizationOwner is a lateinit var, and in should never be null, but for some edge cases on some old
   * instances it can still be missing.
   */
  fun isOrganizationOwnerInitialized(): Boolean {
    return this::organizationOwner.isInitialized
  }

  companion object {
    @Configurable
    class ProjectListener {
      @Autowired
      lateinit var languageService: ObjectFactory<LanguageService>

      @Autowired
      lateinit var projectService: ObjectFactory<ProjectService>

      @PrePersist
      @PreUpdate
      fun preSave(project: Project) {
        if (!(!project::organizationOwner.isInitialized).xor(project.userOwner == null)) {
          throw Exception("Exactly one of organizationOwner or userOwner must be set!")
        }
      }

      @PostLoad
      fun postLoad(project: Project) {
        if (project.baseLanguage == null) {
          languageService.`object`.setNewProjectBaseLanguage(project)
        }
      }
    }
  }
}
