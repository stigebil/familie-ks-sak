package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import java.time.LocalDate

@Entity(name = "FeilutbetaltValuta")
@Table(name = "FEILUTBETALT_VALUTA")
data class FeilutbetaltValuta(
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @Column(name = "fom", columnDefinition = "DATE")
    var fom: LocalDate,
    @Column(name = "tom", columnDefinition = "DATE")
    var tom: LocalDate,
    @Column(name = "feilutbetalt_beloep", nullable = false)
    var feilutbetaltBeløp: Int,

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feilutbetalt_valuta_seq_generator")
    @SequenceGenerator(
        name = "feilutbetalt_valuta_seq_generator",
        sequenceName = "feilutbetalt_valuta_seq",
        allocationSize = 50
    )
    val id: Long = 0
) : BaseEntitet()
