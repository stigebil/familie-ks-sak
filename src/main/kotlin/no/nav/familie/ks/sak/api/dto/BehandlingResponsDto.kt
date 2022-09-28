package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.time.LocalDate
import java.time.LocalDateTime

data class BehandlingResponsDto(
    val behandlingId: Long,
    val steg: BehandlingSteg,
    val stegTilstand: List<BehandlingStegTilstandResponsDto>,
    val status: BehandlingStatus,
    val resultat: Behandlingsresultat,
    val type: BehandlingType,
    val kategori: BehandlingKategori,
    val årsak: BehandlingÅrsak,
    val opprettetTidspunkt: LocalDateTime,
    val endretAv: String,
    val arbeidsfordelingPåBehandling: ArbeidsfordelingResponsDto,
    val personer: List<PersonResponsDto> = emptyList(), // TODO implementeres ved Register Persongrunnlag
    val personResultater: List<PersonResultatResponsDto> = emptyList(), // TODO implementeres ved vilkårsvurdering
    val utbetalingsperioder: List<UtbetalingsperiodeResponsDto> = emptyList(),
    val personerMedAndelerTilkjentYtelse: List<Any> = emptyList(), // TODO implementeres ved tilkjentYtelse
    val endretUtbetalingAndeler: List<Any> = emptyList(), // TODO implementeres ved behandlingsresultat
    val kompetanser: List<Any> = emptyList(), // TODO implementeres ved EØS
    val utenlandskePeriodebeløp: List<Any> = emptyList(), // TODO implementeres ved EØS
    val valutakurser: List<Any> = emptyList() // TODO implementeres ved EØS,
)

data class BehandlingStegTilstandResponsDto(val behandlingSteg: BehandlingSteg, val behandlingStegStatus: BehandlingStegStatus)

data class ArbeidsfordelingResponsDto(
    val behandlendeEnhetId: String,
    val behandlendeEnhetNavn: String,
    val manueltOverstyrt: Boolean = false
)

data class PersonResponsDto(
    val type: PersonType,
    val fødselsdato: LocalDate?,
    val personIdent: String,
    val navn: String,
    val kjønn: KJOENN,
    val registerhistorikk: RegisterHistorikkResponsDto? = null,
    val målform: Målform,
    val dødsfallDato: LocalDate? = null
)

data class RegisterHistorikkResponsDto(
    val hentetTidspunkt: LocalDateTime,
    val sivilstand: List<RegisteropplysningResponsDto>? = emptyList(),
    val oppholdstillatelse: List<RegisteropplysningResponsDto>? = emptyList(),
    val statsborgerskap: List<RegisteropplysningResponsDto>? = emptyList(),
    val bostedsadresse: List<RegisteropplysningResponsDto>? = emptyList(),
    val dødsboadresse: List<RegisteropplysningResponsDto>? = emptyList()
)

data class RegisteropplysningResponsDto(
    val fom: LocalDate?,
    val tom: LocalDate?,
    var verdi: String
)

data class PersonResultatResponsDto(val personIdent: String)