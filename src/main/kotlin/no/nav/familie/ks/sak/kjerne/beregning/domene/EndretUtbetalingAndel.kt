package no.nav.familie.ks.sak.kjerne.beregning.domene

import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.YearMonthConverter
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.StandardbegrunnelseListConverter
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.persistence.Column
import javax.persistence.Convert
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

@Entity(name = "EndretUtbetalingAndel")
@Table(name = "ENDRET_UTBETALING_ANDEL")
data class EndretUtbetalingAndel(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "endret_utbetaling_andel_seq_generator")
    @SequenceGenerator(
        name = "endret_utbetaling_andel_seq_generator",
        sequenceName = "endret_utbetaling_andel_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,

    @ManyToOne
    @JoinColumn(name = "fk_po_person_id")
    var person: Person? = null,

    @Column(name = "prosent")
    var prosent: BigDecimal? = null,

    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var fom: YearMonth? = null,

    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var tom: YearMonth? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak")
    var årsak: Årsak? = null,

    @Column(name = "avtaletidspunkt_delt_bosted")
    var avtaletidspunktDeltBosted: LocalDate? = null,

    @Column(name = "soknadstidspunkt")
    var søknadstidspunkt: LocalDate? = null,

    @Column(name = "begrunnelse")
    var begrunnelse: String? = null,

    @Column(name = "vedtak_begrunnelse_spesifikasjoner")
    @Convert(converter = StandardbegrunnelseListConverter::class)
    var standardbegrunnelser: List<Standardbegrunnelse> = emptyList()
) : BaseEntitet() {

    fun overlapperMed(periode: MånedPeriode) = periode.overlapperHeltEllerDelvisMed(this.periode)

    val periode
        get(): MånedPeriode {
            validerUtfyltEndring()
            return MånedPeriode(checkNotNull(this.fom), checkNotNull(this.tom))
        }

    fun validerUtfyltEndring() {
        if (listOf(person, prosent, fom, tom, årsak, søknadstidspunkt).any { it == null } || (begrunnelse?.isEmpty() == true)) {
            val feilmelding =
                "Person, prosent, fom, tom, årsak, begrunnese og søknadstidspunkt skal være utfylt: $this"
            throw FunksjonellFeil(melding = feilmelding, frontendFeilmelding = feilmelding)
        }

        if (checkNotNull(fom) > checkNotNull(tom)) {
            throw FunksjonellFeil(
                melding = "fom må være lik eller komme før tom",
                frontendFeilmelding = "Du kan ikke sette en f.o.m. dato som er etter t.o.m. dato"
            )
        }

        if (årsak == Årsak.DELT_BOSTED && avtaletidspunktDeltBosted == null) {
            throw FunksjonellFeil("Avtaletidspunkt skal være utfylt når årsak er delt bosted: $this")
        }
    }

    fun erÅrsakDeltBosted() = this.årsak == Årsak.DELT_BOSTED
}

enum class Årsak(val visningsnavn: String) {
    DELT_BOSTED("Delt bosted"),
    ETTERBETALING_3MND("Etterbetaling 3 måneder"),
    ENDRE_MOTTAKER("Foreldrene bor sammen, endret mottaker"),
    ALLEREDE_UTBETALT("Allerede utbetalt")
}