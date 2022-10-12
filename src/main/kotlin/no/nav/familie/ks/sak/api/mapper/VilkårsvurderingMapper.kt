package no.nav.familie.ks.sak.api.mapper

import no.nav.familie.ks.sak.api.dto.AnnenVurderingDto
import no.nav.familie.ks.sak.api.dto.PersonResultatDto
import no.nav.familie.ks.sak.api.dto.VilkårResultatDto
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

object VilkårsvurderingMapper {
    fun lagPersonResultatRespons(personResultat: PersonResultat) = PersonResultatDto(
        personIdent = personResultat.aktør.aktivFødselsnummer(),
        vilkårResultater = personResultat.vilkårResultater.map { lagVilkårResultatRespons(it) },
        andreVurderinger = personResultat.andreVurderinger.map { lagAnnenVurderingRespons(it) }
    )

    private fun lagVilkårResultatRespons(vilkårResultat: VilkårResultat) = VilkårResultatDto(
        resultat = vilkårResultat.resultat,
        erAutomatiskVurdert = vilkårResultat.erAutomatiskVurdert,
        erEksplisittAvslagPåSøknad = vilkårResultat.erEksplisittAvslagPåSøknad,
        id = vilkårResultat.id,
        vilkårType = vilkårResultat.vilkårType,
        periodeFom = vilkårResultat.periodeFom,
        periodeTom = vilkårResultat.periodeTom,
        begrunnelse = vilkårResultat.begrunnelse,
        endretAv = vilkårResultat.endretAv,
        endretTidspunkt = vilkårResultat.endretTidspunkt,
        behandlingId = vilkårResultat.behandlingId,
        erVurdert = vilkårResultat.resultat != Resultat.IKKE_VURDERT || vilkårResultat.versjon > 0,
        avslagBegrunnelser = vilkårResultat.standardbegrunnelser,
        vurderesEtter = vilkårResultat.vurderesEtter,
        utdypendeVilkårsvurderinger = vilkårResultat.utdypendeVilkårsvurderinger
    )

    private fun lagAnnenVurderingRespons(annenVurdering: AnnenVurdering) = AnnenVurderingDto(
        id = annenVurdering.id,
        resultat = annenVurdering.resultat,
        type = annenVurdering.type,
        begrunnelse = annenVurdering.begrunnelse
    )
}
