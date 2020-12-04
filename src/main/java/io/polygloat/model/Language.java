package io.polygloat.model;

import io.polygloat.dtos.request.LanguageDTO;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"repository_id", "name"}, name = "language_repository_name"),
        @UniqueConstraint(columnNames = {"repository_id", "abbreviation"}, name = "language_abbreviation_name")
},
        indexes = {
                @Index(columnList = "abbreviation", name = "index_abbreviation"),
                @Index(columnList = "abbreviation, repository_id", name = "index_abbreviation_repository")
        }
)
public class Language extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "language")
    private Set<Translation> translations;

    @ManyToOne
    private Repository repository;

    private String abbreviation;

    private String name;

    public static Language fromRequestDTO(LanguageDTO dto) {
        Language language = new Language();
        language.setName(dto.getName());
        language.setAbbreviation(dto.getAbbreviation());
        return language;
    }

    public void updateByDTO(LanguageDTO dto) {
        this.name = dto.getName();
        this.abbreviation = dto.getAbbreviation();
    }

    public Repository getRepository() {
        return repository;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String toString() {
        return "Language(id=" + this.getId() + ", abbreviation=" + this.getAbbreviation() + ", name=" + this.getName() + ")";
    }

    public Set<Translation> getTranslations() {
        return this.translations;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTranslations(Set<Translation> translations) {
        this.translations = translations;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public void setName(String name) {
        this.name = name;
    }
}
