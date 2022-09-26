package no.nav.familie.ks.sak.kjerne.behandling.steg

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VedtakSteg : IBehandlingssteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.VEDTAK

    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(VedtakSteg::class.java)
    }
}
