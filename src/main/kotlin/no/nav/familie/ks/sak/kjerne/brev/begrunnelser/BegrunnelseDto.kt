package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet

sealed class BegrunnelseDto(
    open val type: BrevBegrunnelseType,
) : Comparable<BegrunnelseDto> {
    override fun compareTo(other: BegrunnelseDto): Int {
        return when (this) {
            is FritekstBegrunnelseDto -> Int.MAX_VALUE
            is BegrunnelseDtoMedData ->
                when (other) {
                    is FritekstBegrunnelseDto -> this.vedtakBegrunnelseType.sorteringsrekkefølge
                    is BegrunnelseDtoMedData ->
                        when (this.sanityBegrunnelseType) {
                            SanityBegrunnelseType.STANDARD ->
                                if (other.sanityBegrunnelseType == SanityBegrunnelseType.STANDARD) {
                                    this.vedtakBegrunnelseType.sorteringsrekkefølge - other.vedtakBegrunnelseType.sorteringsrekkefølge
                                } else {
                                    -Int.MAX_VALUE
                                }

                            else ->
                                if (other.sanityBegrunnelseType == SanityBegrunnelseType.STANDARD) {
                                    Int.MAX_VALUE
                                } else {
                                    this.vedtakBegrunnelseType.sorteringsrekkefølge - other.vedtakBegrunnelseType.sorteringsrekkefølge
                                }
                        }
                }
        }
    }
}

enum class BrevBegrunnelseType {
    BEGRUNNELSE,
    EØS_BEGRUNNELSE,
    FRITEKST,
}

sealed class BegrunnelseDtoMedData(
    open val apiNavn: String,
    open val vedtakBegrunnelseType: BegrunnelseType,
    open val sanityBegrunnelseType: SanityBegrunnelseType,
    type: BrevBegrunnelseType,
) : BegrunnelseDto(type)

data class BegrunnelseDataDto(
    override val vedtakBegrunnelseType: BegrunnelseType,
    override val apiNavn: String,
    override val sanityBegrunnelseType: SanityBegrunnelseType,
    val gjelderSoker: Boolean,
    val gjelderAndreForelder: Boolean,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maanedOgAarBegrunnelsenGjelderFor: String? = null,
    val maalform: String,
    val belop: String,
    val antallTimerBarnehageplass: String,
    val soknadstidspunkt: String,
) : BegrunnelseDtoMedData(
        apiNavn = apiNavn,
        type = BrevBegrunnelseType.BEGRUNNELSE,
        vedtakBegrunnelseType = vedtakBegrunnelseType,
        sanityBegrunnelseType = sanityBegrunnelseType,
    )

data class FritekstBegrunnelseDto(
    val fritekst: String,
) : BegrunnelseDto(type = BrevBegrunnelseType.FRITEKST)

data class EØSBegrunnelseDataDto(
    override val vedtakBegrunnelseType: BegrunnelseType,
    override val apiNavn: String,
    override val sanityBegrunnelseType: SanityBegrunnelseType,
    val annenForeldersAktivitet: KompetanseAktivitet,
    val annenForeldersAktivitetsland: String?,
    val barnetsBostedsland: String,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maalform: String,
    val sokersAktivitet: KompetanseAktivitet,
    val sokersAktivitetsland: String?,
) : BegrunnelseDtoMedData(
        type = BrevBegrunnelseType.EØS_BEGRUNNELSE,
        apiNavn = apiNavn,
        vedtakBegrunnelseType = vedtakBegrunnelseType,
        sanityBegrunnelseType = sanityBegrunnelseType,
    )
