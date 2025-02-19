package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Håpet er at denne skal kaste feil på sikt, men enn så lenge blir det for strengt. Logger for å se behovet.
fun List<UtvidetVedtaksperiodeMedBegrunnelser>.validerPerioderInneholderBegrunnelser(
    behandlingId: Long,
    fagsakId: Long,
) {
    this.forEach {
        it.validerMinstEnBegrunnelseValgt(behandlingId = behandlingId, fagsakId = fagsakId)
        it.validerMinstEnReduksjonsbegrunnelseVedReduksjon(behandlingId = behandlingId, fagsakId = fagsakId)
        it.validerMinstEnInnvilgetbegrunnelseVedInnvilgelse(behandlingId = behandlingId, fagsakId = fagsakId)
        it.validerMinstEnEndretUtbetalingbegrunnelseVedEndretUtbetaling(
            behandlingId = behandlingId,
            fagsakId = fagsakId,
        )
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnEndretUtbetalingbegrunnelseVedEndretUtbetaling(
    behandlingId: Long,
    fagsakId: Long,
) {
    val erMuligÅVelgeEndretUtbetalingBegrunnelse =
        this.gyldigeBegrunnelser.any { it.begrunnelseType == BegrunnelseType.ENDRET_UTBETALING }
    val erValgtEndretUtbetalingBegrunnelse =
        this.begrunnelser.any { it.begrunnelse.begrunnelseType == BegrunnelseType.ENDRET_UTBETALING }

    if (erMuligÅVelgeEndretUtbetalingBegrunnelse && !erValgtEndretUtbetalingBegrunnelse) {
        logger.warn(
            "Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler endretubetalingsbegrunnelse. Fagsak: $fagsakId, behandling: $behandlingId",
        )
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnInnvilgetbegrunnelseVedInnvilgelse(
    behandlingId: Long,
    fagsakId: Long,
) {
    val erMuligÅVelgeInnvilgetBegrunnelse =
        this.gyldigeBegrunnelser.any { it.begrunnelseType == BegrunnelseType.INNVILGET }
    val erValgtInnvilgetBegrunnelse =
        this.begrunnelser.any { it.begrunnelse.begrunnelseType == BegrunnelseType.INNVILGET }

    if (erMuligÅVelgeInnvilgetBegrunnelse && !erValgtInnvilgetBegrunnelse) {
        logger.warn(
            "Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler innvilgelsebegrunnelse. Fagsak: $fagsakId, behandling: $behandlingId",
        )
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnReduksjonsbegrunnelseVedReduksjon(
    behandlingId: Long,
    fagsakId: Long,
) {
    val erMuligÅVelgeReduksjonBegrunnelse =
        this.gyldigeBegrunnelser.any { it.begrunnelseType == BegrunnelseType.REDUKSJON }
    val erValgtReduksjonBegrunnelse =
        this.begrunnelser.any { it.begrunnelse.begrunnelseType == BegrunnelseType.REDUKSJON }

    if (erMuligÅVelgeReduksjonBegrunnelse && !erValgtReduksjonBegrunnelse) {
        logger.warn(
            "Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler reduksjonsbegrunnelse. Fagsak: $fagsakId, behandling: $behandlingId",
        )
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnBegrunnelseValgt(
    behandlingId: Long,
    fagsakId: Long,
) {
    if (this.begrunnelser.isEmpty()) {
        logger.warn(
            "Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} har ingen begrunnelser knyttet til seg. Fagsak: $fagsakId, behandling: $behandlingId",
        )
    }
}

val logger: Logger = LoggerFactory.getLogger("validerPerioderInneholderBegrunnelserLogger")
