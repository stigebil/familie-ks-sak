package no.nav.familie.ks.sak.api.mapper

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerResponsDto
import no.nav.familie.ks.sak.api.dto.FagsakDeltagerRolle
import no.nav.familie.ks.sak.api.dto.MinimalBehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.MinimalFagsakResponsDto
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak

object FagsakMapper {

    fun lagFagsakDeltagerResponsDto(
        personInfo: PdlPersonInfo? = null,
        ident: String = "",
        rolle: FagsakDeltagerRolle,
        fagsak: Fagsak? = null,
        adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
        harTilgang: Boolean = true
    ): FagsakDeltagerResponsDto = FagsakDeltagerResponsDto(
        navn = personInfo?.navn,
        ident = ident,
        rolle = rolle,
        kjønn = personInfo?.kjønn,
        fagsakId = fagsak?.id,
        fagsakStatus = fagsak?.status,
        adressebeskyttelseGradering = adressebeskyttelseGradering,
        harTilgang = harTilgang
    )

    fun lagMinimalFagsakResponsDto(
        fagsak: Fagsak,
        aktivtBehandling: Behandling? = null,
        behandlinger: List<MinimalBehandlingResponsDto> = emptyList()
    ): MinimalFagsakResponsDto =
        MinimalFagsakResponsDto(
            opprettetTidspunkt = fagsak.opprettetTidspunkt,
            id = fagsak.id,
            søkerFødselsnummer = fagsak.aktør.aktivFødselsnummer(),
            status = fagsak.status,
            underBehandling = if (aktivtBehandling == null) false else aktivtBehandling.status != BehandlingStatus.AVSLUTTET,
            løpendeKategori = aktivtBehandling?.kategori,
            behandlinger = behandlinger
        )

    fun lagBehandlingResponsDto(behandling: Behandling) = MinimalBehandlingResponsDto(
        behandlingId = behandling.id,
        opprettetTidspunkt = behandling.opprettetTidspunkt,
        kategori = behandling.kategori,
        aktiv = behandling.aktiv,
        årsak = behandling.opprettetÅrsak,
        type = behandling.type,
        status = behandling.status,
        resultat = behandling.resultat,
        vedtaksdato = null // TODO - kommer når vedtak er implementert
    )
}