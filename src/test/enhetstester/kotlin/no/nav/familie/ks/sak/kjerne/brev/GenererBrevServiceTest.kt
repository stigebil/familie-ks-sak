package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValutaService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakService
import no.nav.familie.ks.sak.sikkerhet.SaksbehandlerContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ExtendWith(MockKExtension::class)
class GenererBrevServiceTest {
    @MockK
    private lateinit var brevKlient: BrevKlient

    @MockK
    private lateinit var vedtakService: VedtakService

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var simuleringService: SimuleringService

    @MockK
    private lateinit var vedtaksperiodeService: VedtaksperiodeService

    @MockK
    private lateinit var brevPeriodeService: BrevPeriodeService

    @MockK
    private lateinit var totrinnskontrollService: TotrinnskontrollService

    @MockK
    private lateinit var sanityService: SanityService

    @MockK
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @MockK
    private lateinit var korrigertVedtakService: KorrigertVedtakService

    @MockK
    private lateinit var feilutbetaltValutaService: FeilutbetaltValutaService

    @MockK
    private lateinit var saksbehandlerContext: SaksbehandlerContext

    @InjectMockKs
    private lateinit var genererBrevService: GenererBrevService

    private val søker = randomAktør()
    private val fagsak = lagFagsak(søker)
    private val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val manueltBrevDto =
        ManueltBrevDto(
            brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
            mottakerIdent = søker.aktivFødselsnummer(),
            multiselectVerdier = listOf("Dokumentasjon som viser når barna kom til Norge."),
            enhet = Enhet("id", "id"),
        )

    @ParameterizedTest
    @EnumSource(
        value = Brevmal::class,
        names = ["VEDTAK_FØRSTEGANGSVEDTAK", "VEDTAK_ENDRING", "VEDTAK_OPPHØRT", "VEDTAK_OPPHØR_MED_ENDRING", "VEDTAK_AVSLAG", "VEDTAK_FORTSATT_INNVILGET", "VEDTAK_KORREKSJON_VEDTAKSBREV", "VEDTAK_OPPHØR_DØDSFALL", "AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG", "AUTOVEDTAK_NYFØDT_FØRSTE_BARN", "AUTOVEDTAK_NYFØDT_BARN_FRA_FØR"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `genererManueltBrev - skal ikke journalføre brev for brevmaler som ikke kan sendes manuelt`(brevmal: Brevmal) {
        every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "test"

        val feil =
            assertThrows<Feil> {
                genererBrevService.genererManueltBrev(
                    ManueltBrevDto(
                        brevmal = brevmal,
                        mottakerIdent = søker.aktivFødselsnummer(),
                    ),
                )
            }
        assertEquals("Kan ikke mappe fra manuel brevrequest til $brevmal.", feil.message)
    }

    @Test
    fun `hentForhåndsvisningAvBrev - skal kaste feil dersom kall mot 'familie-brev' feiler`() {
        every { personopplysningGrunnlagService.hentSøker(behandlingId = behandling.id) } returns
            lagPerson(
                lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søker.aktivFødselsnummer()),
                søker,
            )
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetNavn = "Behandlende enhet",
                behandlendeEnhetId = "1234",
            )

        every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "test"

        every {
            brevKlient.genererBrev(any(), any())
        } throws Exception("Kall mot familie-brev feilet")

        val feil =
            assertThrows<Feil> { genererBrevService.genererManueltBrev(manueltBrevDto, true) }
        assertEquals(
            "Klarte ikke generere brev for ${manueltBrevDto.brevmal}. Kall mot familie-brev feilet",
            feil.message,
        )
        assertEquals(
            "Det har skjedd en feil. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
            feil.frontendFeilmelding,
        )
    }
}
