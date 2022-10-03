package no.nav.familie.ks.sak.kjerne.søknad

import no.nav.familie.ks.sak.kjerne.søknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.søknad.domene.SøknadGrunnlagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SøknadGrunnlagService(
    private val søknadGrunnlagRepository: SøknadGrunnlagRepository
) {
    @Transactional
    fun lagreOgDeaktiverGammel(søknadGrunnlag: SøknadGrunnlag): SøknadGrunnlag {
        søknadGrunnlagRepository.hentAktiv(søknadGrunnlag.behandlingId)
            ?.let { søknadGrunnlagRepository.saveAndFlush(it.also { it.aktiv = false }) }

        return søknadGrunnlagRepository.save(søknadGrunnlag)
    }

    fun hentAktiv(behandlingId: Long): SøknadGrunnlag? {
        return søknadGrunnlagRepository.hentAktiv(behandlingId)
    }
}
