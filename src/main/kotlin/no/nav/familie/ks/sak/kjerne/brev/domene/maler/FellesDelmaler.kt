package no.nav.familie.ks.sak.kjerne.brev.domene.maler

import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext

data class SignaturDelmal(
    val enhet: Flettefelt,
    val saksbehandler: Flettefelt = flettefelt(SikkerhetContext.hentSaksbehandlerNavn())
) {

    constructor(enhet: String) : this(flettefelt(enhet))
}

data class SignaturVedtak(
    val enhet: Flettefelt,
    val saksbehandler: Flettefelt,
    val beslutter: Flettefelt
) {

    constructor(enhet: String, saksbehandler: String, beslutter: String) : this(
        flettefelt(enhet),
        flettefelt(saksbehandler),
        flettefelt(beslutter)
    )
}

data class Etterbetaling(
    val etterbetalingsbelop: Flettefelt
) {

    constructor(etterbetalingsbeløp: String) : this(
        flettefelt(etterbetalingsbeløp)
    )
}

data class Hjemmeltekst(
    val hjemler: Flettefelt
) {

    constructor(hjemler: String) : this(
        flettefelt(hjemler)
    )
}

data class AutoUnderskrift(
    val enhet: Flettefelt
) {

    constructor(enhet: String) : this(
        flettefelt(enhet)
    )
}