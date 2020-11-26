package io.polygloat.model;

import io.polygloat.dtos.request.LanguageDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
@ToString(of = {"id", "abbreviation", "name"})
public class Language extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "language")
    @Getter
    @Setter
    private Set<Translation> translations;

    @ManyToOne
    @Setter
    private Repository repository;

    @Setter
    private String abbreviation;

    @Setter
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
}
