package no.nav.familie.ks.sak.statistikk.stønadsstatistikk

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.familie.eksterne.kontrakter.Vilkår
import no.nav.familie.ks.sak.common.util.førsteDagINesteMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårsvurderingOppfylt
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
internal class StønadsstatistikkServiceTest {
    @MockK(relaxed = true)
    private lateinit var behandlingHentOgPersisterService: BehandlingService

    @MockK
    private lateinit var persongrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var vedtakService: VedtakService

    @MockK
    private lateinit var personopplysningerService: PersonOpplysningerService

    @MockK
    private lateinit var andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @InjectMockKs
    private lateinit var stønadsstatistikkService: StønadsstatistikkService

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val søkerFnr = behandling.fagsak.aktør.aktivFødselsnummer()
    private val barnFnr = randomFnr()
    private val barn2Fnr = randomFnr()

    private val personopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barnFnr, barn2Fnr),
            søkerAktør = behandling.fagsak.aktør,
        ).also {
            it.personer.forEach { it.bostedsadresser = mutableListOf(mockk(), mockk()) }
        }
    private val barn1 = personopplysningGrunnlag.barna.first()
    private val barn2 = personopplysningGrunnlag.barna.last()

    private val vilkårsVurdering = lagVilkårsvurderingOppfylt(personopplysningGrunnlag.personer, behandling, false)

    @Test
    fun `hentVedtakDVH skal kaste feil dersom vedtak ikke har noe dato satt`() {
        val vedtak = Vedtak(behandling = behandling)

        every { behandlingHentOgPersisterService.hentBehandling(any()) } returns behandling
        every { persongrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vedtakService.hentAktivVedtakForBehandling(any()) } returns vedtak
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(any()) } returns "DK"
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns
            listOf(mockk())
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns vilkårsVurdering

        val exception =
            assertThrows<IllegalStateException> {
                stønadsstatistikkService.hentVedtakDVH(1L)
            }

        assertEquals(exception.message, "Fant ikke vedtaksdato for behandling 1")
    }

    @Test
    fun `hentVedtakDVH skal hente og generere VedtakDVH med riktige detaljer om utbetalinger og at vilkårsresultater foreligger`() {
        val vedtak = Vedtak(behandling = behandling, vedtaksdato = LocalDateTime.now())

        val andelTilkjentYtelseBarn1 =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = barn1.fødselsdato.førsteDagINesteMåned().toYearMonth(),
                    stønadTom = barn1.fødselsdato.plusYears(3).toYearMonth(),
                    sats = 1054,
                    aktør = barn1.aktør,
                    periodeOffset = 1,
                ),
                emptyList(),
            )

        val andelTilkjentYtelseBarn2PeriodeMed0Beløp =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = barn2.fødselsdato.førsteDagINesteMåned().toYearMonth(),
                    stønadTom = barn2.fødselsdato.plusYears(3).toYearMonth(),
                    sats = 0,
                    aktør = barn2.aktør,
                ),
                emptyList(),
            )

        val andelTilkjentYtelseSøker =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = barn2.fødselsdato.førsteDagINesteMåned().toYearMonth(),
                    stønadTom = barn2.fødselsdato.plusYears(3).toYearMonth(),
                    sats = 50,
                    aktør = barn2.aktør,
                ),
                emptyList(),
            )

        val andelerTilkjentYtelse =
            listOf(
                andelTilkjentYtelseBarn1,
                andelTilkjentYtelseBarn2PeriodeMed0Beløp,
                andelTilkjentYtelseSøker,
            )

        every { behandlingHentOgPersisterService.hentBehandling(any()) } returns behandling
        every { persongrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vedtakService.hentAktivVedtakForBehandling(any()) } returns vedtak
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(any()) } returns "DK"
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns
            andelerTilkjentYtelse
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns vilkårsVurdering
        every { persongrunnlagService.hentBarna(any()) } returns personopplysningGrunnlag.barna

        val vedtakDvh = stønadsstatistikkService.hentVedtakDVH(1L)

        assertEquals(2, vedtakDvh.utbetalingsperioder[0].utbetalingsDetaljer.size)

        vedtakDvh.utbetalingsperioder
            .flatMap { it.utbetalingsDetaljer.map { ud -> ud.person } }
            .filter { it.personIdent != søkerFnr }
            .forEach {
                assertEquals(0, it.delingsprosentYtelse)
            }

        assertEquals(true, vedtakDvh.vilkårResultater?.isNotEmpty())

        assertTrue(vedtakDvh.vilkårResultater!!.any { it.vilkårType == Vilkår.BARNEHAGEPLASS })
    }
}
