package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ks.sak.common.tidslinje.beskjærEtter
import no.nav.familie.ks.sak.common.tidslinje.inneholder
import no.nav.familie.ks.sak.common.tidslinje.tomTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

class VilkårsvurderingTidslinjer(
    vilkårsvurdering: Vilkårsvurdering,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
) {
    private val barna: List<Aktør> = personopplysningGrunnlag.barna.map { it.aktør }
    private val søker: Aktør = personopplysningGrunnlag.søker.aktør

    private val aktørTilPersonResultater = vilkårsvurdering.personResultater.associateBy { it.aktør }

    private val vilkårResultaterTidslinjeMap =
        aktørTilPersonResultater.entries.associate { (aktør, personResultat) ->
            aktør to
                personResultat.vilkårResultater.groupBy { it.vilkårType }
                    .map { it.value.tilVilkårRegelverkResultatTidslinje() }
        }

    private val søkersTidslinje: SøkersTidslinjer = SøkersTidslinjer(this, søker)

    private val barnasTidslinjer: Map<Aktør, BarnetsTidslinjer> =
        barna.associateWith { BarnetsTidslinjer(this, it) }

    fun barnasTidslinjer(): Map<Aktør, BarnetsTidslinjer> = barnasTidslinjer.entries.associate { it.key to it.value }

    class SøkersTidslinjer(tidslinjer: VilkårsvurderingTidslinjer, aktør: Aktør) {
        val vilkårResultatTidslinjer = tidslinjer.vilkårResultaterTidslinjeMap[aktør] ?: listOf(tomTidslinje())
        val regelverkResultatTidslinje =
            vilkårResultatTidslinjer.kombiner {
                kombinerVilkårResultaterTilRegelverkResultat(PersonType.SØKER, it)
            }
    }

    class BarnetsTidslinjer(tidslinjer: VilkårsvurderingTidslinjer, aktør: Aktør) {
        private val søkersTidslinje = tidslinjer.søkersTidslinje

        val vilkårResultatTidslinjer = tidslinjer.vilkårResultaterTidslinjeMap[aktør] ?: listOf(tomTidslinje())
        val egetRegelverkResultatTidslinje =
            vilkårResultatTidslinjer.kombiner {
                kombinerVilkårResultaterTilRegelverkResultat(PersonType.BARN, it)
            }
        val regelverkResultatTidslinje =
            egetRegelverkResultatTidslinje
                .kombinerMed(søkersTidslinje.regelverkResultatTidslinje) { barnetsResultat, søkersResultat ->
                    barnetsResultat.kombinerMed(søkersResultat)
                }.beskjærEtter(søkersTidslinje.regelverkResultatTidslinje)
    }

    fun harBlandetRegelverk(): Boolean {
        return søkersTidslinje.regelverkResultatTidslinje.inneholder(RegelverkResultat.OPPFYLT_BLANDET_REGELVERK) ||
            barnasTidslinjer().values.any {
                it.egetRegelverkResultatTidslinje.inneholder(RegelverkResultat.OPPFYLT_BLANDET_REGELVERK)
            }
    }
}
