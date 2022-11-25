package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import forskyvVilkårResultater
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.Trigger
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import tilFørskjøvetVilkårResultatTidslinjeMap

class FinnGyldigeBegrunnelserForPeriodeContext(
    private val utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    private val sanityBegrunnelser: List<SanityBegrunnelse>,
    private val persongrunnlag: PersonopplysningGrunnlag,
    private val personResultater: List<PersonResultat>,
    private val aktørIderMedUtbetaling: List<String>
) {

    private val vedtaksperiode = Periode(
        fom = utvidetVedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
        tom = utvidetVedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
    )

    fun hentGyldigeBegrunnelserForVedtaksperiode(): List<Begrunnelse> {
        val tillateBegrunnelserForVedtakstype = Begrunnelse.values()
            .filter {
                utvidetVedtaksperiodeMedBegrunnelser
                    .type
                    .tillatteBegrunnelsestyper
                    .contains(it.begrunnelseType)
            }

        return when (utvidetVedtaksperiodeMedBegrunnelser.type) {
            Vedtaksperiodetype.FORTSATT_INNVILGET,
            Vedtaksperiodetype.AVSLAG -> tillateBegrunnelserForVedtakstype

            Vedtaksperiodetype.UTBETALING,
            Vedtaksperiodetype.OPPHØR -> tillateBegrunnelserForVedtakstype.filtrerPasserVedtaksperiode()
        }
    }

    private fun List<Begrunnelse>.filtrerPasserVedtaksperiode(): List<Begrunnelse> {
        val begrunnelserSomTriggesForVedtaksperiode =
            filter { it.begrunnelseType != BegrunnelseType.FORTSATT_INNVILGET }
                .filter { it.triggesForVedtaksperiode() }

        val fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget =
            utvidetVedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.UTBETALING && begrunnelserSomTriggesForVedtaksperiode.isEmpty()

        return if (fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget) {
            filter { it.begrunnelseType == BegrunnelseType.FORTSATT_INNVILGET }
        } else {
            begrunnelserSomTriggesForVedtaksperiode
        }
    }

    private fun Begrunnelse.triggesForVedtaksperiode(): Boolean {
        val sanityBegrunnelse = this.tilSanityBegrunnelse(sanityBegrunnelser) ?: return false

        val vilkårResultaterSomPasserVedtaksperioden: Map<Person, List<VilkårResultat>> =
            hentVilkårResultaterSomOverlapperVedtaksperiode(this)
                .filtrerPersonerUtenUtbetalingVedInnvilget(this.begrunnelseType)
                .filtrerPåVilkårType(sanityBegrunnelse.vilkår)
                .filtrerPåTriggere(sanityBegrunnelse.triggere, sanityBegrunnelse.type)
                .filtrerPåUtdypendeVilkårsvurdering(
                    sanityBegrunnelse.utdypendeVilkårsvurderinger,
                    sanityBegrunnelse.type
                )
                .filtrerPåVilkårResultaterSomPasserMedVedtaksperiodeDatoEllerSanityBegrunnelseType(
                    finnVilkårResultatIderSomPasserMedVedtaksperiodeDato(),
                    sanityBegrunnelse.type
                )

        return vilkårResultaterSomPasserVedtaksperioden.isNotEmpty()
    }

    private fun Map<Person, List<VilkårResultat>>.filtrerPåVilkårResultaterSomPasserMedVedtaksperiodeDatoEllerSanityBegrunnelseType(
        vilkårResultaterSomPasserMedVedtaksperiodeDato: List<Long>,
        begrunnelseType: SanityBegrunnelseType
    ) = this.mapValues {
        it.value.filter { vilkårResultat ->
            vilkårResultaterSomPasserMedVedtaksperiodeDato.contains(
                vilkårResultat.id
            ) || begrunnelseType == SanityBegrunnelseType.STANDARD
        }
    }.filterValues { it.isNotEmpty() }

    private fun Begrunnelse.finnVilkårResultatIderSomPasserMedVedtaksperiodeDato() =
        when (this.begrunnelseType) {
            BegrunnelseType.REDUKSJON,
            BegrunnelseType.EØS_INNVILGET,
            BegrunnelseType.AVSLAG,
            BegrunnelseType.ENDRET_UTBETALING,
            BegrunnelseType.INNVILGET -> finnVilkårResultaterSomStarterSamtidigSomPeriode()

            BegrunnelseType.EØS_OPPHØR,
            BegrunnelseType.ETTER_ENDRET_UTBETALING,
            BegrunnelseType.OPPHØR -> finnVilkårResultaterSomSlutterFørPeriode()

            BegrunnelseType.FORTSATT_INNVILGET -> throw Feil("FORTSATT_INNVILGET skal være filtrert bort.")
        }

    private fun finnVilkårResultaterSomStarterSamtidigSomPeriode() =
        personResultater.flatMap { personResultat ->
            val vilkårTilVilkårResultaterMap = personResultat.vilkårResultater.groupBy { it.vilkårType }

            vilkårTilVilkårResultaterMap.mapValues { (vilkår, vilkårResultater) ->
                forskyvVilkårResultater(vilkår, vilkårResultater).filter { it.fom == vedtaksperiode.fom }
                    .map { it.verdi.id }
            }.filterValues { it.isNotEmpty() }.flatMap { it.value }
        }

    private fun finnVilkårResultaterSomSlutterFørPeriode() =
        personResultater.flatMap { personResultat ->
            val vilkårTilVilkårResultaterMap = personResultat.vilkårResultater.groupBy { it.vilkårType }

            vilkårTilVilkårResultaterMap.mapValues { (vilkår, vilkårResultater) ->
                forskyvVilkårResultater(vilkår, vilkårResultater).filter { it.tom?.plusDays(1) == vedtaksperiode.fom }
                    .map { it.verdi.id }
            }.filterValues { it.isNotEmpty() }.flatMap { it.value }
        }

    private fun hentVilkårResultaterSomOverlapperVedtaksperiode(standardBegrunnelse: Begrunnelse) =
        when (standardBegrunnelse.begrunnelseType) {
            BegrunnelseType.REDUKSJON,
            BegrunnelseType.EØS_INNVILGET,
            BegrunnelseType.AVSLAG,
            BegrunnelseType.ENDRET_UTBETALING,
            BegrunnelseType.INNVILGET -> finnPersonerMedVilkårResultaterSomGjelderIPeriode()

            BegrunnelseType.EØS_OPPHØR,
            BegrunnelseType.ETTER_ENDRET_UTBETALING,
            BegrunnelseType.OPPHØR -> finnPersonerMedVilkårResultaterSomGjelderRettFørPeriode()

            BegrunnelseType.FORTSATT_INNVILGET -> throw Feil("FORTSATT_INNVILGET skal være filtrert bort.")
        }

    private fun finnPersonerMedVilkårResultaterSomGjelderIPeriode(): Map<Person, List<VilkårResultat>> =

        personResultater.tilFørskjøvetVilkårResultatTidslinjeMap(persongrunnlag)
            .mapNotNull { (aktør, vilkårResultatTidslinjeForPerson) ->
                val person =
                    persongrunnlag.personer.find { it.aktør.aktivFødselsnummer() == aktør.aktivFødselsnummer() }
                val forskøvedeVilkårResultaterMedSammeFom =
                    vilkårResultatTidslinjeForPerson.tilPerioderIkkeNull().singleOrNull {
                        it.fom == vedtaksperiode.fom
                    }?.verdi
                if (person != null && forskøvedeVilkårResultaterMedSammeFom != null) {
                    Pair(person, forskøvedeVilkårResultaterMedSammeFom)
                } else {
                    null
                }
            }.toMap().filterValues { it.isNotEmpty() }

    private fun finnPersonerMedVilkårResultaterSomGjelderRettFørPeriode(): Map<Person, List<VilkårResultat>> =
        personResultater.tilFørskjøvetVilkårResultatTidslinjeMap(persongrunnlag).mapNotNull { (aktør, tidsjlinje) ->
            val person = persongrunnlag.personer.find { it.aktør.aktivFødselsnummer() == aktør.aktivFødselsnummer() }
            val forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode =
                tidsjlinje.tilPerioderIkkeNull().singleOrNull {
                    it.tom?.plusDays(1) == vedtaksperiode.fom
                }?.verdi

            if (person != null && forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode != null) {
                Pair(person, forskøvedeVilkårResultaterSlutterDagenFørVedtaksperiode)
            } else {
                null
            }
        }.toMap().filterValues { it.isNotEmpty() }

    private fun Map<Person, List<VilkårResultat>>.filtrerPersonerUtenUtbetalingVedInnvilget(begrunnelseType: BegrunnelseType) =
        this.filterKeys {
            begrunnelseType != BegrunnelseType.INNVILGET ||
                aktørIderMedUtbetaling.contains(it.aktør.aktørId) ||
                it.type == PersonType.SØKER
        }

    private fun Map<Person, List<VilkårResultat>>.filtrerPåVilkårType(vilkårTyperFraSanity: List<Vilkår>) =
        this.mapValues { (_, vilkårResultaterForPerson) ->
            vilkårResultaterForPerson.filter { vilkårTyperFraSanity.contains(it.vilkårType) }
        }.filterValues { it.isNotEmpty() }

    private fun Map<Person, List<VilkårResultat>>.filtrerPåTriggere(
        triggereFraSanity: List<Trigger>,
        sanityBegrunnelseType: SanityBegrunnelseType
    ) = this.filter { (person, vilkårResultaterForPerson) ->
        val oppfylteTriggereIBehandling =
            Trigger.values().filter { it.erOppfylt(vilkårResultaterForPerson, person) }

        // Strengere logikk for Standardbegrunnelsene for innvilgelse
        if (sanityBegrunnelseType == SanityBegrunnelseType.STANDARD) {
            oppfylteTriggereIBehandling == triggereFraSanity
        } else {
            triggereFraSanity.all { oppfylteTriggereIBehandling.contains(it) }
        }
    }.filterValues { it.isNotEmpty() }

    private fun Map<Person, List<VilkårResultat>>.filtrerPåUtdypendeVilkårsvurdering(
        utdypendeVilkårFraSanity: List<UtdypendeVilkårsvurdering>,
        sanityBegrunnelseType: SanityBegrunnelseType
    ) =
        this.filterValues { vilkårResultaterForPerson ->
            val utdypendeVilkårIBehandling =
                vilkårResultaterForPerson.flatMap { it.utdypendeVilkårsvurderinger }.toSet()

            if (sanityBegrunnelseType == SanityBegrunnelseType.STANDARD) {
                utdypendeVilkårIBehandling == utdypendeVilkårFraSanity.toSet()
            } else {
                utdypendeVilkårFraSanity.all { utdypendeVilkårIBehandling.contains(it) }
            }
        }
}