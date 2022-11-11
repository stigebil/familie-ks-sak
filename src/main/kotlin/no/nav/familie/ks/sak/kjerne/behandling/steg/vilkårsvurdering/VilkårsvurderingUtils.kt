package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.VedtakBegrunnelseTilknyttetVilkårResponseDto
import no.nav.familie.ks.sak.api.dto.VilkårResultatDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.IkkeNullbarPeriode
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.diffIDager
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erBack2BackIMånedsskifte
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityEØSBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.tilTriggesAv
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.EØSStandardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.tilSanityEØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.PeriodeResultat
import no.nav.familie.ks.sak.kjerne.beregning.tilPeriodeResultater
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.time.LocalDate
import java.time.Month

fun standardbegrunnelserTilNedtrekksmenytekster(
    sanityBegrunnelser: List<SanityBegrunnelse>
) =
    Standardbegrunnelse
        .values()
        .groupBy { it.vedtakBegrunnelseType }
        .mapValues { begrunnelseGruppe ->
            begrunnelseGruppe.value
                .flatMap { vedtakBegrunnelse ->
                    vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
                        sanityBegrunnelser,
                        vedtakBegrunnelse
                    )
                }
        }

fun eøsStandardbegrunnelserTilNedtrekksmenytekster(
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>
) = EØSStandardbegrunnelse.values().groupBy { it.vedtakBegrunnelseType }
    .mapValues { begrunnelseGruppe ->
        begrunnelseGruppe.value.flatMap { vedtakBegrunnelse ->
            eøsBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
                sanityEØSBegrunnelser,
                vedtakBegrunnelse
            )
        }
    }

fun vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    vedtakBegrunnelse: Standardbegrunnelse
): List<VedtakBegrunnelseTilknyttetVilkårResponseDto> {
    val sanityBegrunnelse = vedtakBegrunnelse.tilSanityBegrunnelse(sanityBegrunnelser) ?: return emptyList()

    val triggesAv = sanityBegrunnelse.tilTriggesAv()
    val visningsnavn = sanityBegrunnelse.navnISystem

    return if (triggesAv.vilkår.isEmpty()) {
        listOf(
            VedtakBegrunnelseTilknyttetVilkårResponseDto(
                id = vedtakBegrunnelse,
                navn = visningsnavn,
                vilkår = null
            )
        )
    } else {
        triggesAv.vilkår.map {
            VedtakBegrunnelseTilknyttetVilkårResponseDto(
                id = vedtakBegrunnelse,
                navn = visningsnavn,
                vilkår = it
            )
        }
    }
}

fun eøsBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>,
    vedtakBegrunnelse: EØSStandardbegrunnelse
): List<VedtakBegrunnelseTilknyttetVilkårResponseDto> {
    val eøsSanityBegrunnelse = vedtakBegrunnelse.tilSanityEØSBegrunnelse(sanityEØSBegrunnelser) ?: return emptyList()

    return listOf(
        VedtakBegrunnelseTilknyttetVilkårResponseDto(
            id = vedtakBegrunnelse,
            navn = eøsSanityBegrunnelse.navnISystem,
            vilkår = null
        )
    )
}

/**
 * Funksjon som tar inn et endret vilkår og lager nye vilkårresultater til å få plass til den endrede perioden.
 * @param[eksisterendeVilkårResultater] Eksisterende vilkårresultater
 * @param[endretVilkårResultatDto] Endret vilkårresultat
 * @return VilkårResultater før og etter mutering
 */
fun endreVilkårResultat(
    eksisterendeVilkårResultater: List<VilkårResultat>,
    endretVilkårResultatDto: VilkårResultatDto
): List<VilkårResultat> {
    validerAvslagUtenPeriodeMedLøpende(
        eksisterendeVilkårResultater = eksisterendeVilkårResultater,
        endretVilkårResultat = endretVilkårResultatDto
    )

    val endretVilkårResultat =
        endretVilkårResultatDto.tilVilkårResultat(eksisterendeVilkårResultater.single { it.id == endretVilkårResultatDto.id })

    val (vilkårResultaterSomSkalTilpasses, vilkårResultaterSomIkkeTrengerTilpassning) = eksisterendeVilkårResultater.partition {
        !it.erAvslagUtenPeriode() || it.id == endretVilkårResultatDto.id
    }

    val tilpassetVilkårResultater = vilkårResultaterSomSkalTilpasses
        .flatMap {
            tilpassVilkårForEndretVilkår(
                endretVilkårResultatId = endretVilkårResultatDto.id,
                eksisterendeVilkårResultat = it,
                endretVilkårResultat = endretVilkårResultat
            )
        }

    return tilpassetVilkårResultater + vilkårResultaterSomIkkeTrengerTilpassning
}

/**
 * Funksjon som forsøker å legge til en periode på et vilkår.
 * Dersom det allerede finnes en uvurdet periode med samme vilkårstype
 * skal det kastes en feil.
 */
fun opprettNyttVilkårResultat(personResultat: PersonResultat, vilkårType: Vilkår): VilkårResultat {
    if (harUvurdertePerioderForVilkårType(personResultat, vilkårType)) {
        throw FunksjonellFeil(
            melding = "Det finnes allerede uvurderte vilkår av samme vilkårType",
            frontendFeilmelding = "Du må ferdigstille vilkårsvurderingen på en periode som allerede er påbegynt, før du kan legge til en ny periode"
        )
    }

    return VilkårResultat(
        personResultat = personResultat,
        vilkårType = vilkårType,
        resultat = Resultat.IKKE_VURDERT,
        begrunnelse = "",
        behandlingId = personResultat.vilkårsvurdering.behandling.id
    )
}

/**
 * @param [endretVilkårResultatId] id til VilkårResultat som er endret
 * @param [eksisterendeVilkårResultat] vilkårresultat som skal oppdaters på person
 * @param [endretVilkårResultat] endret VilkårResultat
 */
fun tilpassVilkårForEndretVilkår(
    endretVilkårResultatId: Long,
    eksisterendeVilkårResultat: VilkårResultat,
    endretVilkårResultat: VilkårResultat
): List<VilkårResultat> {
    if (eksisterendeVilkårResultat.id == endretVilkårResultatId) {
        return listOf(endretVilkårResultat)
    }

    if (eksisterendeVilkårResultat.vilkårType != endretVilkårResultat.vilkårType || endretVilkårResultat.erAvslagUtenPeriode()) {
        return listOf(eksisterendeVilkårResultat)
    }

    val eksisterendeVilkårResultatTidslinje = listOf(eksisterendeVilkårResultat).tilTidslinje()
    val endretVilkårResultatTidslinje = listOf(endretVilkårResultat).tilTidslinje()

    return eksisterendeVilkårResultatTidslinje
        .kombinerMed(endretVilkårResultatTidslinje) { eksisterendeVilkår, endretVilkår ->
            if (endretVilkår != null) {
                null
            } else {
                eksisterendeVilkår
            }
        }.tilPerioderIkkeNull()
        .map {
            it.tilVilkårResultatMedOppdatertPeriodeOgBehandlingsId(nyBehandlingsId = endretVilkårResultat.behandlingId)
        }
}

private fun harUvurdertePerioderForVilkårType(personResultat: PersonResultat, vilkårType: Vilkår): Boolean =
    personResultat.vilkårResultater.any { it.vilkårType == vilkårType && it.resultat == Resultat.IKKE_VURDERT }

private fun validerAvslagUtenPeriodeMedLøpende(
    eksisterendeVilkårResultater: List<VilkårResultat>,
    endretVilkårResultat: VilkårResultatDto
) {
    val filtrerteVilkårResultater =
        eksisterendeVilkårResultater.filter { it.vilkårType == endretVilkårResultat.vilkårType && it.id != endretVilkårResultat.id }

    when {
        // For bor med søker-vilkåret kan avslag og innvilgelse være overlappende, da man kan f.eks. avslå full kontantstøtte, men innvilge delt
        endretVilkårResultat.vilkårType == Vilkår.BOR_MED_SØKER -> return

        endretVilkårResultat.erAvslagUtenPeriode() && filtrerteVilkårResultater.any { it.resultat == Resultat.OPPFYLT && it.harFremtidigTom() } ->
            throw FunksjonellFeil(
                "Finnes løpende oppfylt ved forsøk på å legge til avslag uten periode ",
                "Du kan ikke legge til avslag uten datoer fordi det finnes oppfylt løpende periode på vilkåret."
            )

        endretVilkårResultat.harFremtidigTom() && filtrerteVilkårResultater.any { it.erAvslagUtenPeriode() } ->
            throw FunksjonellFeil(
                "Finnes avslag uten periode ved forsøk på å legge til løpende oppfylt",
                "Du kan ikke legge til løpende periode fordi det er vurdert avslag uten datoer på vilkåret."
            )
    }
}

fun List<VilkårResultat>.tilTidslinje(): Tidslinje<VilkårResultat> {
    return map {
        Periode(
            verdi = it,
            fom = it.periodeFom,
            tom = it.periodeTom
        )
    }.tilTidslinje()
}

private fun Periode<VilkårResultat>.tilVilkårResultatMedOppdatertPeriodeOgBehandlingsId(
    nyBehandlingsId: Long
): VilkårResultat {
    val vilkårResultat = this.verdi

    val vilkårsdatoErUendret = this.fom == vilkårResultat.periodeFom && this.tom == vilkårResultat.periodeTom

    return if (vilkårsdatoErUendret) {
        vilkårResultat
    } else {
        vilkårResultat.kopierMedNyPeriodeOgBehandling(
            fom = this.fom,
            tom = this.tom,
            behandlingId = nyBehandlingsId
        )
    }
}

fun finnTilOgMedDato(tilOgMed: LocalDate?, vilkårResultater: List<VilkårResultat>): LocalDate {
    // LocalDateTimeline krasjer i isTimelineOutsideInterval funksjonen dersom vi sender med TIDENES_ENDE,
    // så bruker tidenes ende minus én dag.
    if (tilOgMed == null) return TIDENES_ENDE.minusDays(1)
    val skalVidereføresEnMndEkstra = vilkårResultater.any { vilkårResultat ->
        erBack2BackIMånedsskifte(tilOgMed = tilOgMed, fraOgMed = vilkårResultat.periodeFom)
    }

    return if (skalVidereføresEnMndEkstra) tilOgMed.plusMonths(1).sisteDagIMåned() else tilOgMed.sisteDagIMåned()
}

fun validerBarnasVilkår(vilkårsvurdering: Vilkårsvurdering, barna: List<Person>) {
    val feil = mutableListOf<String>()

    barna.map { barn ->
        vilkårsvurdering.personResultater
            .flatMap { it.vilkårResultater }
            .filter { it.personResultat?.aktør == barn.aktør }
            .forEach { vilkårResultat ->
                val fødselsdato = barn.fødselsdato.tilDagMånedÅr()
                val vilkårType = vilkårResultat.vilkårType
                if (vilkårResultat.resultat == Resultat.OPPFYLT && vilkårResultat.periodeFom == null) {
                    feil.add("Vilkår $vilkårType for barn med fødselsdato $fødselsdato mangler fom dato.")
                }
                if (vilkårResultat.periodeFom != null &&
                    vilkårResultat.lagOgValiderPeriodeFraVilkår().fom.isBefore(barn.fødselsdato)
                ) {
                    feil.add(
                        "Vilkår $vilkårType for barn med fødselsdato $fødselsdato " +
                            "har fom dato før barnets fødselsdato."
                    )
                }
                if (vilkårResultat.periodeFom != null &&
                    vilkårResultat.erEksplisittAvslagPåSøknad != true &&
                    vilkårResultat.vilkårType == Vilkår.MELLOM_1_OG_2_ELLER_ADOPTERT
                ) {
                    vilkårResultat.validerVilkår_MELLOM_1_OG_2_ELLER_ADOPTERT(
                        vilkårResultat.lagOgValiderPeriodeFraVilkår(),
                        barn.fødselsdato
                    )?.let { feil.add(it) }
                }
            }
    }

    if (feil.isNotEmpty()) {
        throw Feil(feil.joinToString(separator = "\n"))
    }
}

private fun VilkårResultat.lagOgValiderPeriodeFraVilkår(): IkkeNullbarPeriode<Long> = when {
    periodeFom !== null -> {
        IkkeNullbarPeriode(verdi = behandlingId, fom = checkNotNull(periodeFom), tom = periodeTom ?: TIDENES_ENDE)
    }

    erEksplisittAvslagPåSøknad == true && periodeTom == null -> {
        IkkeNullbarPeriode(verdi = behandlingId, fom = TIDENES_MORGEN, tom = TIDENES_ENDE)
    }

    else -> {
        throw FunksjonellFeil("Ugyldig periode. Periode må ha t.o.m.-dato eller være et avslag uten datoer.")
    }
}

private fun VilkårResultat.validerVilkår_MELLOM_1_OG_2_ELLER_ADOPTERT(
    periode: IkkeNullbarPeriode<Long>,
    barnFødselsdato: LocalDate
): String? = when {
    this.erAdopsjonOppfylt() &&
        periode.tom.isAfter(barnFødselsdato.plusYears(6).withMonth(Month.AUGUST.value).sisteDagIMåned()) ->
        "Du kan ikke sette en t.o.m dato som er etter august året barnet fyller 6 år."

    this.erAdopsjonOppfylt() && periode.fom.diffIDager(periode.tom) > 365 ->
        "Differansen mellom f.o.m datoen og t.o.m datoen kan ikke være mer enn 1 år."
    !this.erAdopsjonOppfylt() && !periode.fom.isEqual(barnFødselsdato.plusYears(1)) ->
        "F.o.m datoen må være lik barnets 1 års dag."
    !this.erAdopsjonOppfylt() && !periode.tom.isEqual(barnFødselsdato.plusYears(2)) ->
        "T.o.m datoen må være lik barnets 2 års dag."
    else -> null
}

fun hentInnvilgedePerioder(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering
): Pair<List<PeriodeResultat>, List<PeriodeResultat>> {
    val periodeResultater = vilkårsvurdering.personResultater.flatMap { it.tilPeriodeResultater() }

    val barnaIdenter = personopplysningGrunnlag.barna.map { it.aktør }

    val innvilgedePeriodeResultaterSøker = periodeResultater.filter {
        it.aktør == personopplysningGrunnlag.søker.aktør && it.allePåkrevdeVilkårErOppfylt(PersonType.SØKER)
    }
    val innvilgedePeriodeResultaterBarna = periodeResultater.filter {
        barnaIdenter.contains(it.aktør) && it.allePåkrevdeVilkårErOppfylt(PersonType.BARN)
    }
    return innvilgedePeriodeResultaterSøker to innvilgedePeriodeResultaterBarna
}

fun genererVilkårsvurderingFraForrigeVedtattBehandling(
    initiellVilkårsvurdering: Vilkårsvurdering,
    forrigeBehandlingVilkårsvurdering: Vilkårsvurdering,
    personopplysningGrunnlag: PersonopplysningGrunnlag
): Vilkårsvurdering {
    val vilkårsvurdering = kopierVilkårResultaterFraGammelTilNyVilkårsvurdering(
        forrigeBehandlingVilkårsvurdering = forrigeBehandlingVilkårsvurdering,
        initiellVilkårsvurdering = initiellVilkårsvurdering
    )

    vilkårsvurdering.personResultater.forEach { personResultat ->
        val person = personopplysningGrunnlag.personer.single { it.aktør == personResultat.aktør }
        val dødsDato = person.dødsfall?.dødsfallDato

        if (dødsDato != null) {
            val vilkårResultaterOppdatertMedDødsfalldato =
                hentVilkårResultaterOppdatertMedDødsfalldato(personResultat.vilkårResultater, dødsDato)

            personResultat.setSortedVilkårResultater(vilkårResultaterOppdatertMedDødsfalldato)
        }
    }

    return vilkårsvurdering
}

fun hentVilkårResultaterOppdatertMedDødsfalldato(
    vilkårResultater: MutableSet<VilkårResultat>,
    dødsDato: LocalDate
): MutableSet<VilkårResultat> {
    return vilkårResultater
        .filter { dødsDato.isAfter(it.periodeFom) }
        .map {
            val erDødsdatoFørVilkårsperiodeSlutter =
                dødsDato.isBefore(it.periodeTom ?: TIDENES_ENDE)
            if (erDødsdatoFørVilkårsperiodeSlutter) {
                it.periodeTom = dødsDato
                it.begrunnelse = "Dødsfall"

                it
            } else {
                it
            }
        }.toMutableSet()
}
