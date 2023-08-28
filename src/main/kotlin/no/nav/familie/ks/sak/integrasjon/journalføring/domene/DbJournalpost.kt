package no.nav.familie.ks.sak.integrasjon.journalføring.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import java.time.LocalDateTime
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

@Entity(name = "Journalpost")
@Table(name = "JOURNALPOST")
data class DbJournalpost(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journalpost_seq_generator")
    @SequenceGenerator(name = "journalpost_seq_generator", sequenceName = "journalpost_seq", allocationSize = 50)
    val id: Long = 0,

    @Column(name = "opprettet_av", nullable = false, updatable = false)
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerNavn(),

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_behandling_id", nullable = false)
    val behandling: Behandling,

    @Column(name = "journalpost_id")
    val journalpostId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    val type: DbJournalpostType? = null,
) {
    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DbJournalpost

        if (id != other.id) return false

        return true
    }
}

enum class DbJournalpostType {
    I, U
}
