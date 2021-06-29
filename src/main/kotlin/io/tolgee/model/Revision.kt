package io.tolgee.model

import org.hibernate.annotations.GenericGenerator
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import java.io.Serializable
import javax.persistence.*

@Entity
@RevisionEntity(RevisionListener::class)
class Revision : Serializable {

    @Id
    @GenericGenerator(
            name = "revisionSequenceGenerator",
            strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "revisionSequenceGenerator"
    )
    @RevisionNumber
    private val id = 0

    @RevisionTimestamp
    private val timestamp: Long = 0

    @OneToOne
    var author: UserAccount? = null
}
