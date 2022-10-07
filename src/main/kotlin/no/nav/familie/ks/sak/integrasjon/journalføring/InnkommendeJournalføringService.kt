package no.nav.familie.ks.sak.integrasjon.journalføring

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.LogiskVedleggRequest
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.api.dto.JournalføringRequestDto
import no.nav.familie.ks.sak.api.dto.JournalpostDokumentDto
import no.nav.familie.ks.sak.api.dto.OppdaterJournalpostRequestDto
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.api.dto.Sakstype
import no.nav.familie.ks.sak.api.dto.tilOppdaterJournalpostRequestDto
import no.nav.familie.ks.sak.common.secureLogger
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpost
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.DbJournalpostType
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.transaction.Transactional

@Service
class InnkommendeJournalføringService(
    private val integrasjonClient: IntegrasjonClient,
    private val fagsakService: FagsakService,
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val journalføringRepository: JournalføringRepository,
    private val loggService: LoggService
) {

    fun hentJournalposterForBruker(brukerId: String): List<Journalpost> = integrasjonClient.hentJournalposterForBruker(
        JournalposterForBrukerRequest(
            antall = 1000, brukerId = Bruker(id = brukerId, type = BrukerIdType.FNR), tema = listOf(Tema.KON)
        )
    )

    fun hentJournalpost(journalpostId: String): Journalpost = integrasjonClient.hentJournalpost(journalpostId)

    fun hentDokumentIJournalpost(journalpostId: String, dokumentId: String): ByteArray =
        integrasjonClient.hentDokumentIJournalpost(dokumentId, journalpostId)

    @Transactional
    fun journalfør(
        request: JournalføringRequestDto,
        journalpostId: String,
        oppgaveId: String
    ): String {
        val tilknyttedeBehandlingIder = request.tilknyttedeBehandlingIder.toMutableList()

        if (request.opprettOgKnyttTilNyBehandling) {
            val nyBehandling = opprettBehandlingOgEvtFagsakForJournalføring(
                personIdent = request.bruker.id,
                saksbehandlerIdent = request.navIdent,
                type = request.nyBehandlingstype,
                årsak = request.nyBehandlingsårsak,
                kategori = request.kategori,
                søknadMottattDato = request.datoMottatt?.toLocalDate(),
            )

            tilknyttedeBehandlingIder.add(nyBehandling.id.toString())
        }

        val (tilknyttetFagsak, behandlinger) = lagreJournalpostOgKnyttFagsakTilJournalpost(
            tilknyttedeBehandlingIder, journalpostId
        )

        oppdaterLogiskeVedlegg(request.dokumenter)

        oppdaterOgFerdigstill(
            oppdaterJournalPostRequest = request.tilOppdaterJournalpostRequestDto(tilknyttetFagsak),
            journalpostId = journalpostId,
            behandlendeEnhet = request.journalførendeEnhet,
            oppgaveId = oppgaveId,
            behandlinger = behandlinger
        )

        return tilknyttetFagsak.fagsakId ?: ""
    }

    private fun opprettBehandlingOgEvtFagsakForJournalføring(
        personIdent: String,
        saksbehandlerIdent: String,
        type: BehandlingType,
        årsak: BehandlingÅrsak,
        kategori: BehandlingKategori? = null,
        søknadMottattDato: LocalDate? = null,
    ): Behandling {
        fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(personIdent))

        val nyBehandlingDto = OpprettBehandlingDto(
            kategori = kategori,
            søkersIdent = personIdent,
            behandlingType = type,
            behandlingÅrsak = årsak,
            saksbehandlerIdent = saksbehandlerIdent,
            søknadMottattDato = søknadMottattDato,
        )

        return stegService.håndterNyBehandling(nyBehandlingDto)
    }

    private fun lagreJournalpostOgKnyttFagsakTilJournalpost(
        tilknyttedeBehandlingIder: List<String>,
        journalpostId: String
    ): Pair<Sak, List<Behandling>> {
        val behandlinger = tilknyttedeBehandlingIder.map { behandlingService.hentBehandling(it.toLong()) }
        val journalpost = hentJournalpost(journalpostId)

        behandlinger.forEach {
            journalføringRepository.save(
                DbJournalpost(
                    behandling = it,
                    journalpostId = journalpostId,
                    type = DbJournalpostType.valueOf(journalpost.journalposttype.name)
                )
            )
        }

        val fagsak = behandlinger.map { it.fagsak }.toSet().singleOrNull()

        val tilknyttetFagsak = Sak(
            fagsakId = fagsak?.id?.toString(),
            fagsaksystem = fagsak?.let { Tema.KON.name },
            sakstype = fagsak?.let { Sakstype.FAGSAK.type } ?: Sakstype.GENERELL_SAK.type
        )

        return Pair(tilknyttetFagsak, behandlinger)
    }

    private fun oppdaterLogiskeVedlegg(dokumenter: List<JournalpostDokumentDto>) {
        dokumenter.forEach { dokument ->
            val eksisterendeLogiskeVedlegg = dokument.eksisterendeLogiskeVedlegg ?: emptyList()
            val logiskeVedlegg = dokument.logiskeVedlegg ?: emptyList()

            val fjernedeVedlegg = eksisterendeLogiskeVedlegg.filter { !logiskeVedlegg.contains(it) }
            val nyeVedlegg = logiskeVedlegg.filter { !eksisterendeLogiskeVedlegg.contains(it) }

            fjernedeVedlegg.forEach {
                integrasjonClient.slettLogiskVedlegg(it.logiskVedleggId, dokument.dokumentInfoId)
            }

            nyeVedlegg.forEach {
                integrasjonClient.leggTilLogiskVedlegg(LogiskVedleggRequest(it.tittel), dokument.dokumentInfoId)
            }
        }
    }

    private fun oppdaterOgFerdigstill(
        oppdaterJournalPostRequest: OppdaterJournalpostRequestDto,
        journalpostId: String,
        behandlendeEnhet: String,
        oppgaveId: String,
        behandlinger: List<Behandling>
    ) {
        runCatching {
            secureLogger.info("Oppdaterer journalpost $journalpostId med $oppdaterJournalPostRequest")

            integrasjonClient.oppdaterJournalpost(oppdaterJournalPostRequest, journalpostId)

            opprettLoggPåDokumenter(journalpostId, behandlinger)

            secureLogger.info("Ferdigstiller journalpost $journalpostId")

            integrasjonClient.ferdigstillJournalpost(
                journalpostId = journalpostId, journalførendeEnhet = behandlendeEnhet
            )

            integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
        }.onFailure {
            hentJournalpost(journalpostId).journalstatus.apply {
                if (this == Journalstatus.FERDIGSTILT) {
                    integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
                } else {
                    throw it
                }
            }
        }
    }

    private fun opprettLoggPåDokumenter(journalpostId: String, behandlinger: List<Behandling>) {
        val journalpost = hentJournalpost(journalpostId)

        val loggTekst = journalpost.dokumenter?.fold("") { loggTekst, dokumentInfo ->
            loggTekst + "${dokumentInfo.tittel}" + dokumentInfo.logiskeVedlegg?.fold("") { logiskeVedleggTekst, logiskVedlegg -> logiskeVedleggTekst + "\n\u2002\u2002${logiskVedlegg.tittel}" } + "\n"
        } ?: ""

        behandlinger.forEach {
            loggService.opprettMottattDokumentLogg(
                behandling = it, tekst = loggTekst, mottattDato = journalpost.datoMottatt!!
            )
        }
    }
}
