package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.BEHANDLINGSRESULTAT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.IVERKSETT_MOT_OPPDRAG
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.REGISTRERE_PERSONGRUNNLAG
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.REGISTRERE_SØKNAD
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.VILKÅRSVURDERING
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.KLAR
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.TILBAKEFØRT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.UTFØRT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.VENTER
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class StegServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var stegService: StegService

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun setup() {
        val aktør = aktørRepository.saveAndFlush(randomAktør())
        fagsak = fagsakRepository.saveAndFlush(lagFagsak(aktør))
    }

    @Test
    fun `utførSteg skal utføre registerpersongrunnlag og flytter til register søknad for førstegangsbehandling`() {
        var behandling = behandlingRepository.saveAndFlush(
            lagBehandling(
                fagsak = fagsak,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD
            )
        )

        assertSteg(behandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_PERSONGRUNNLAG) }

        behandling = behandlingRepository.finnBehandling(behandling.id)
        assertEquals(2, behandling.behandlingStegTilstand.size)
        assertSteg(behandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertSteg(behandling, REGISTRERE_SØKNAD, KLAR)
    }

    @Test
    fun `utførSteg skal utføre registerpersongrunnlag og flytter til vilkårsvurdering for revurdering med årsak NYE_OPPLYSNINGER`() {
        var behandling = behandlingRepository.saveAndFlush(
            lagBehandling(
                fagsak = fagsak,
                type = BehandlingType.REVURDERING,
                opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER
            )
        )
        assertSteg(behandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_PERSONGRUNNLAG) }

        behandling = behandlingRepository.finnBehandling(behandling.id)
        assertEquals(2, behandling.behandlingStegTilstand.size)
        assertSteg(behandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertSteg(behandling, VILKÅRSVURDERING, KLAR)
    }

    @Test
    fun `utførSteg skal tilbakeføre behandlingsresultat når registersøknad utføres på nytt for førstegangsbehandling`() {
        var behandling = behandlingRepository.saveAndFlush(
            lagBehandling(
                fagsak = fagsak,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD
            )
        )
        assertSteg(behandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_PERSONGRUNNLAG) }
        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_SØKNAD) }
        assertDoesNotThrow { stegService.utførSteg(behandling.id, VILKÅRSVURDERING) }

        behandling = behandlingRepository.finnBehandling(behandling.id)
        assertEquals(4, behandling.behandlingStegTilstand.size)
        assertSteg(behandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertSteg(behandling, REGISTRERE_SØKNAD, UTFØRT)
        assertSteg(behandling, VILKÅRSVURDERING, UTFØRT)
        assertSteg(behandling, BEHANDLINGSRESULTAT, KLAR)

        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_SØKNAD) }
        behandling = behandlingRepository.finnBehandling(behandling.id)
        assertEquals(4, behandling.behandlingStegTilstand.size)
        assertSteg(behandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertSteg(behandling, REGISTRERE_SØKNAD, UTFØRT)
        assertSteg(behandling, VILKÅRSVURDERING, KLAR)
        assertSteg(behandling, BEHANDLINGSRESULTAT, TILBAKEFØRT)
    }

    @Test
    fun `utførSteg skal gjenoppta registersøknad når steget er på vent for førstegangsbehandling`() {
        var behandling = behandlingRepository.saveAndFlush(
            lagBehandling(
                fagsak = fagsak,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD
            )
        )
        assertSteg(behandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_PERSONGRUNNLAG) }
        behandling = behandlingRepository.finnBehandling(behandling.id)
            .also { it.behandlingStegTilstand.last().behandlingStegStatus = VENTER }
        behandlingRepository.saveAndFlush(behandling)

        behandling = behandlingRepository.finnBehandling(behandling.id)
        assertEquals(2, behandling.behandlingStegTilstand.size)
        assertSteg(behandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertSteg(behandling, REGISTRERE_SØKNAD, VENTER)

        assertDoesNotThrow { stegService.utførSteg(behandling.id, REGISTRERE_SØKNAD) }
        behandling = behandlingRepository.finnBehandling(behandling.id)
        assertEquals(2, behandling.behandlingStegTilstand.size)
        assertSteg(behandling, REGISTRERE_PERSONGRUNNLAG, UTFØRT)
        assertSteg(behandling, REGISTRERE_SØKNAD, KLAR)
    }

    @Test
    fun `utførSteg skal ikke utføre iverksettmotOppdrag steg`() {
        val behandling = behandlingRepository.saveAndFlush(
            lagBehandling(
                fagsak = fagsak,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD
            )
        )
        behandling.leggTilNySteg(IVERKSETT_MOT_OPPDRAG)
        behandlingRepository.saveAndFlush(behandling)

        val exception = assertThrows<RuntimeException> { stegService.utførSteg(behandling.id, IVERKSETT_MOT_OPPDRAG) }
        assertEquals(
            "Steget ${IVERKSETT_MOT_OPPDRAG.name} kan ikke behandles for behandling ${behandling.id}",
            exception.message
        )
    }

    @Test
    fun `utførSteg skal ikke utføre registersøknad for behandling med årsak satsendring`() {
        val behandling = behandlingRepository.saveAndFlush(
            lagBehandling(
                fagsak = fagsak,
                type = BehandlingType.REVURDERING,
                opprettetÅrsak = BehandlingÅrsak.SATSENDRING
            )
        )
        assertSteg(behandling, REGISTRERE_PERSONGRUNNLAG, KLAR)
        behandling.leggTilNySteg(REGISTRERE_SØKNAD)
        behandlingRepository.saveAndFlush(behandling)

        val exception = assertThrows<RuntimeException> { stegService.utførSteg(behandling.id, REGISTRERE_SØKNAD) }
        assertEquals(
            "Steget ${REGISTRERE_SØKNAD.name} er ikke gyldig for behandling ${behandling.id} " +
                "med opprettetÅrsak ${behandling.opprettetÅrsak}",
            exception.message
        )
    }

    @Test
    fun `utførSteg skal ikke utføre registersøknad steg før registerpersongrunnlag er utført`() {
        val behandling = behandlingRepository.saveAndFlush(
            lagBehandling(
                fagsak = fagsak,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD
            )
        )
        behandling.leggTilNySteg(REGISTRERE_SØKNAD)
        behandlingRepository.saveAndFlush(behandling)

        val exception = assertThrows<RuntimeException> { stegService.utførSteg(behandling.id, REGISTRERE_SØKNAD) }
        assertEquals(
            "Behandling ${behandling.id} har allerede et steg " +
                "${REGISTRERE_PERSONGRUNNLAG.name}} som er klar for behandling. " +
                "Kan ikke behandle ${REGISTRERE_SØKNAD.name}",
            exception.message
        )
    }

    private fun assertSteg(behandling: Behandling, behandlingSteg: BehandlingSteg, behandlingStegStatus: BehandlingStegStatus) =
        assertTrue(
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == behandlingSteg &&
                    it.behandlingStegStatus == behandlingStegStatus
            }
        )
}
