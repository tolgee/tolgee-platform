package com.polygloat.model;

import lombok.*;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"source_id", "language_id"}, name = "translation_source_language"),
})
@Data
public class Translation extends AuditModel {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(columnDefinition = "text")
    private String text;

    @ManyToOne
    private Source source;

    @ManyToOne
    private Language language;

}
