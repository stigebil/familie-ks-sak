package no.nav.familie.ks.sak.kjerne.behandling.steg

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.BesluttVedtakDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.FeatureToggleConfig
import no.nav.familie.ks.sak.config.FeatureToggleService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.prosessering.internal.TaskService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class BeslutteVedtakStegTest {
    @MockK

    private lateinit var totrinnskontrollService: TotrinnskontrollService

    @MockK
    private lateinit var vedtakService: VedtakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var taskService: TaskService

    @MockK
    private lateinit var loggService: LoggService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @MockK
    private lateinit var featureToggleService: FeatureToggleService

    @InjectMockKs
    private lateinit var beslutteVedtakSteg: BeslutteVedtakSteg

    @Test
    fun `utførSteg skal kaste FunksjonellFeil dersom behandlingen er satt til IVERKSETTER_VEDTAK `() {
        every { behandlingService.hentBehandling(200) } returns lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).apply {
            status = BehandlingStatus.IVERKSETTER_VEDTAK
        }

        val funksjonellFeil = assertThrows<FunksjonellFeil> { beslutteVedtakSteg.utførSteg(200, mockk()) }

        assertThat(funksjonellFeil.message, Is("Behandlingen er allerede sendt til oppdrag og venter på kvittering"))
    }

    @Test
    fun `utførSteg skal kaste FunksjonellFeil dersom behandlingen er satt til AVSLUTTET `() {
        every { behandlingService.hentBehandling(200) } returns lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).apply {
            status = BehandlingStatus.AVSLUTTET
        }

        val funksjonellFeil = assertThrows<FunksjonellFeil> { beslutteVedtakSteg.utførSteg(200, mockk()) }

        assertThat(funksjonellFeil.message, Is("Behandlingen er allerede avsluttet"))
    }

    @Test
    fun `utførSteg skal kaste FunksjonellFeil dersom behandling årsaken er satt til KORREKSJON_VEDTAKSBREV og SB ikke har feature togglet på `() {
        every { behandlingService.hentBehandling(200) } returns lagBehandling(opprettetÅrsak = BehandlingÅrsak.KORREKSJON_VEDTAKSBREV)
        every { featureToggleService.isEnabled(FeatureToggleConfig.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV) } returns false

        val funksjonellFeil = assertThrows<FunksjonellFeil> { beslutteVedtakSteg.utførSteg(200, mockk()) }

        assertThat(
            funksjonellFeil.message,
            Is("Årsak Korrigere vedtak med egen brevmal og toggle familie-ks-sak.behandling.korreksjon-vedtaksbrev false")
        )
        assertThat(
            funksjonellFeil.frontendFeilmelding,
            Is("Du har ikke tilgang til å beslutte for denne behandlingen. Ta kontakt med teamet dersom dette ikke stemmer.")
        )
    }

    @Test
    fun `utførSteg skal opprette FerdigstillGodkjenneVedtak task og logg dersom vedtaket er godkjent `() {
        val besluttVedtakDto = BesluttVedtakDto(Beslutning.GODKJENT, "GODKJENT")

        every { behandlingService.hentBehandling(200) } returns lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        every { featureToggleService.isEnabled(FeatureToggleConfig.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV) } returns false
        every {
            totrinnskontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                besluttVedtakDto.beslutning,
                emptyList()
            )
        } returns mockk(relaxed = true)
        every { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any()) } returns mockk()
        every { taskService.save(any()) } returns mockk()

        beslutteVedtakSteg.utførSteg(200, besluttVedtakDto)

        verify(exactly = 1) {
            totrinnskontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                besluttVedtakDto.beslutning,
                emptyList()
            )
        }
        verify(exactly = 1) { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any()) }
        verify(exactly = 1) { taskService.save(any()) }
    }
}