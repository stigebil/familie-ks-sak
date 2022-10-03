package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.sivilstand

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.time.LocalDate
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "GrSivilstand")
@Table(name = "PO_SIVILSTAND")
data class GrSivilstand(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_sivilstand_seq_generator")
    @SequenceGenerator(
        name = "po_sivilstand_seq_generator",
        sequenceName = "po_sivilstand_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "fom")
    val fom: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: SIVILSTAND,

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
    val person: Person
) : BaseEntitet() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrSivilstand

        return !(
            fom != other.fom ||
                type != other.type ||
                person != other.person
            )
    }

    override fun hashCode() = Objects.hash(fom, type, person)

    companion object {
        fun fraSivilstand(sivilstand: Sivilstand, person: Person) =
            GrSivilstand(
                fom = sivilstand.gyldigFraOgMed,
                type = sivilstand.type,
                person = person
            )
    }
}