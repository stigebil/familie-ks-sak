package no.nav.familie.ks.sak.kjerne.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.EndreVilkårResultatDto
import no.nav.familie.ks.sak.api.dto.NyttVilkårDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårsvurderingService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val personidentService: PersonidentService
) {

    fun opprettVilkårsvurdering(behandling: Behandling, forrigeBehandlingSomErVedtatt: Behandling?): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering for behandling ${behandling.id}")

        val aktivVilkårsvurdering = finnAktivVilkårsvurdering(behandling.id)

        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
        val initiellVilkårsvurdering = genererInitiellVilkårsvurdering(behandling, personopplysningGrunnlag)

        val finnesVilkårsvurderingPåInneværendeBehandling = aktivVilkårsvurdering != null
        val førsteVilkårsvurderingPåBehandlingOgFinnesTidligereVedtattBehandling =
            forrigeBehandlingSomErVedtatt != null && !finnesVilkårsvurderingPåInneværendeBehandling

        var vilkårsvurdering = initiellVilkårsvurdering

        if (førsteVilkårsvurderingPåBehandlingOgFinnesTidligereVedtattBehandling) {
            // vilkårsvurdering = genererVilkårsvurderingBasertPåTidligereVilkårsvurdering(initiellVilkårsvurdering, forrigeBehandlingSomErVedtatt)
            // TODO: implementer generering av vilkårsvurdering basert på tidligere vilkårsvurdering
        }

        return lagreVilkårsvurdering(vilkårsvurdering, aktivVilkårsvurdering)
    }

    private fun lagreVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
        aktivVilkårsvurdering: Vilkårsvurdering?
    ): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} lagrer vilkårsvurdering $vilkårsvurdering")
        aktivVilkårsvurdering?.let { vilkårsvurderingRepository.saveAndFlush(it.also { it.aktiv = false }) }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun finnAktivVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? =
        vilkårsvurderingRepository.finnAktivForBehandling(behandlingId)

    private fun genererInitiellVilkårsvurdering(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ): Vilkårsvurdering {
        return Vilkårsvurdering(behandling = behandling).apply {
            personResultater = personopplysningGrunnlag.personer.map { person ->
                val personResultat = PersonResultat(vilkårsvurdering = this, aktør = person.aktør)

                val vilkårForPerson = Vilkår.hentVilkårFor(person.type)

                val vilkårResultater = vilkårForPerson.map { vilkår ->
                    VilkårResultat(
                        personResultat = personResultat,
                        erAutomatiskVurdert = true,
                        resultat = Resultat.IKKE_VURDERT,
                        vilkårType = vilkår,
                        begrunnelse = "",
                        behandlingId = behandling.id
                    )
                }.toSortedSet(VilkårResultat.VilkårResultatComparator)

                personResultat.setSortedVilkårResultater(vilkårResultater)

                personResultat
            }.toSet()
        }
    }

    @Transactional
    fun endreVilkår(
        behandlingId: Long,
        endreVilkårResultatDto: EndreVilkårResultatDto
    ) {
        val vilkårsvurdering = hentAktivForBehandling(behandlingId)
        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, endreVilkårResultatDto.personIdent)
        val vilkårResultater = personResultat.vilkårResultater

        val nyeVilkårResultater =
            endreVilkårResultat(vilkårResultater.toList(), endreVilkårResultatDto.endretVilkårResultat)

        // Vilkårresultatene trenger ikke å eksplitt save pga @Transactional
        vilkårResultater.clear()
        vilkårResultater.addAll(nyeVilkårResultater)
    }

    @Transactional
    fun opprettNyttVilkårPåBehandling(behandlingId: Long, nyttVilkårDto: NyttVilkårDto) {
        val vilkårsvurdering = hentAktivForBehandling(behandlingId)
        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, nyttVilkårDto.personIdent)
        val vilkårResultater = personResultat.vilkårResultater

        val nyttVilkår = opprettNyttVilkårResultat(personResultat, nyttVilkårDto.vilkårType)

        // Vilkårresultatene trenger ikke å eksplitt save pga @Transactional
        vilkårResultater.add(nyttVilkår)
    }

    @Transactional
    fun slettEllerNullstillVilkår(behandlingId: Long, vilkårId: Long, aktør: Aktør) {
        val vilkårsvurdering = hentAktivForBehandling(behandlingId)

        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, aktør.aktivFødselsnummer())

        val vilkårResultater = personResultat.vilkårResultater

        val vilkårResultat = vilkårResultater.find { it.id == vilkårId }
            ?: throw Feil(
                message = "Prøver å slette et vilkår som ikke finnes",
                frontendFeilmelding = "Vilkåret du prøver å slette finnes ikke i systemet."
            )

        val perioderMedSammeVilkårType = vilkårResultater
            .filter { it.vilkårType == vilkårResultat.vilkårType && it.id != vilkårResultat.id }

        vilkårResultater.remove(vilkårResultat)

        if (perioderMedSammeVilkårType.isEmpty()) {
            val nyttVilkårMedNullstilteFelter = opprettNyttVilkårResultat(personResultat, vilkårResultat.vilkårType)
            vilkårResultater.add(nyttVilkårMedNullstilteFelter)
        }
    }

    fun hentAktivForBehandling(behandlingId: Long): Vilkårsvurdering = finnAktivVilkårsvurdering(behandlingId)
        ?: throw Feil("Fant ikke vilkårsvurdering knyttet til behandling=$behandlingId")

    private fun finnPersonResultatForPersonThrows(
        personResultater: Set<PersonResultat>,
        personIdent: String
    ): PersonResultat {
        val aktør = personidentService.hentAktør(personIdent)

        return personResultater.find { it.aktør == aktør } ?: throw Feil(
            message = "Fant ikke vilkårsvurdering for person",
            frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident $personIdent"
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(VilkårsvurderingService::class.java)
    }
}
