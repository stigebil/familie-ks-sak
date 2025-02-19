package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class VedtaksperiodeUtilTest {
    @ParameterizedTest
    @EnumSource(
        value = Vedtaksperiodetype::class,
        names = ["OPPHØR", "AVSLAG"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `validerVedtaksperiodeMedBegrunnelser - skal kaste FunksjonellFeil dersom det er fritekst uten stanadard begrunnelser i opphør eller avslag`(
        vedtaksperiodetype: Vedtaksperiodetype,
    ) {
        val vedtaksperiodeMedBegrunnelser =
            VedtaksperiodeMedBegrunnelser(
                vedtak = mockk(),
                type = vedtaksperiodetype,
                fritekster = mutableListOf(mockk()),
            )

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser)
            }

        assertThat(
            funksjonellFeil.message,
            Is(
                "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. Legg først til en ny begrunnelse eller fjern friteksten(e).",
            ),
        )
    }

    @Test
    fun `validerVedtaksperiodeMedBegrunnelser - skal kaste FunksjonellFeil dersom det eksisterer både fritekst og begrunnelser`() {
        val vedtaksperiodeMedBegrunnelser =
            VedtaksperiodeMedBegrunnelser(
                vedtak = mockk<Vedtak>().also { every { it.behandling.resultat } returns Behandlingsresultat.FORTSATT_INNVILGET },
                type = Vedtaksperiodetype.FORTSATT_INNVILGET,
                fritekster = mutableListOf(mockk()),
                begrunnelser = mutableSetOf(mockk()),
            )

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser)
            }

        assertThat(
            funksjonellFeil.message,
            Is("Det ble sendt med både fritekst og begrunnelse. Vedtaket skal enten ha fritekst eller begrunnelse, men ikke begge deler."),
        )
    }
}
