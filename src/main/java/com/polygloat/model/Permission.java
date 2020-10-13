package com.polygloat.model;

import com.polygloat.constants.ApiScope;
import lombok.*;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true, of = {"id"})
@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne()
    private UserAccount user;

    @OneToOne
    private Invitation invitation;

    @ManyToOne
    private Repository repository;

    @Enumerated(EnumType.STRING)
    private RepositoryPermissionType type;

    @AllArgsConstructor
    public enum RepositoryPermissionType {
        VIEW(1, new ApiScope[]{ApiScope.TRANSLATIONS_VIEW}),
        TRANSLATE(2, new ApiScope[]{ApiScope.TRANSLATIONS_VIEW, ApiScope.TRANSLATIONS_EDIT}),
        EDIT(3, new ApiScope[]{ApiScope.TRANSLATIONS_VIEW, ApiScope.TRANSLATIONS_EDIT, ApiScope.SOURCES_EDIT}),
        MANAGE(4, new ApiScope[]{ApiScope.TRANSLATIONS_VIEW, ApiScope.TRANSLATIONS_EDIT, ApiScope.SOURCES_EDIT});

        @Getter
        private final int power;

        @Getter
        private final ApiScope[] availableScopes;
    }
}
