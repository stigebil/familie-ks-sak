package no.nav.familie.ks.sak.kjerne.vedtak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.vedtak.domene.VedtakRepository
import org.springframework.stereotype.Service

@Service
class VedtakService(private val vedtakRepository: VedtakRepository) {

    fun opprettOgInitierNyttVedtakForBehandling(behandling: Behandling) {
        behandling.steg.takeUnless { it !== BehandlingSteg.BESLUTTE_VEDTAK && it !== BehandlingSteg.REGISTRERE_PERSONGRUNNLAG }
            ?: throw Feil("Forsøker å initiere vedtak på steg ${behandling.steg}")

        vedtakRepository.findByBehandlingAndAktivOptional(behandlingId = behandling.id)
            ?.let { vedtakRepository.saveAndFlush(it.also { it.aktiv = false }) }

        vedtakRepository.save(Vedtak(behandling = behandling))
    }
}