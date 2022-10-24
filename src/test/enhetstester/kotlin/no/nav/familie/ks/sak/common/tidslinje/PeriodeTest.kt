package no.nav.familie.ks.sak.common.tidslinje

import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PeriodeTest {

    private val førsteJanuar = LocalDate.of(2022, 1, 1)
    private val sisteDagIJanuar = LocalDate.of(2022, 1, 31)
    private val førsteFebruar = LocalDate.of(2022, 2, 1)
    private val sisteDagIFebruar = LocalDate.of(2022, 2, 28)
    private val førsteMars = LocalDate.of(2022, 3, 1)
    private val sisteDagIMars = LocalDate.of(2022, 3, 31)

    @Test
    fun `tilPerioder - Skal beholde datoer ved manipulering på tidslinje`() {
        val tidslinjeA = listOf(Periode("a", førsteJanuar, sisteDagIMars)).tilTidslinje()
        val tidslinjeB = listOf(Periode("b", førsteFebruar, sisteDagIFebruar)).tilTidslinje()

        val periode = tidslinjeA.kombinerMed(tidslinjeB) { a, b ->
            b ?: a
        }.tilPerioder()

        Assertions.assertEquals(3, periode.size)

        Assertions.assertEquals(førsteJanuar, periode[0].fom)
        Assertions.assertEquals(sisteDagIJanuar, periode[0].tom)

        Assertions.assertEquals(førsteFebruar, periode[1].fom)
        Assertions.assertEquals(sisteDagIFebruar, periode[1].tom)

        Assertions.assertEquals(førsteMars, periode[2].fom)
        Assertions.assertEquals(sisteDagIMars, periode[2].tom)
    }

    @Test
    fun `tilPerioder - Skal kunne håndtere splitt i tidslinje`() {
        val perioder = listOf(
            Periode("a", førsteJanuar, sisteDagIJanuar),
            Periode("c", førsteMars, sisteDagIMars)
        ).tilTidslinje().tilPerioder()

        Assertions.assertEquals(3, perioder.size)

        Assertions.assertEquals(førsteJanuar, perioder[0].fom)
        Assertions.assertEquals(sisteDagIJanuar, perioder[0].tom)
        Assertions.assertEquals("a", perioder[0].verdi)

        Assertions.assertEquals(førsteFebruar, perioder[1].fom)
        Assertions.assertEquals(sisteDagIFebruar, perioder[1].tom)
        Assertions.assertEquals(null, perioder[1].verdi)

        Assertions.assertEquals(førsteMars, perioder[2].fom)
        Assertions.assertEquals(sisteDagIMars, perioder[2].tom)
        Assertions.assertEquals("c", perioder[2].verdi)
    }

    @Test
    fun `tilPerioder - Skal kunne håndtere nullverdier i starten og slutten av tidslinje`() {
        val perioder = listOf(
            Periode("a", null, sisteDagIJanuar),
            Periode("b", førsteFebruar, sisteDagIFebruar),
            Periode("c", førsteMars, null)
        ).tilTidslinje().tilPerioder()

        Assertions.assertEquals(3, perioder.size)

        Assertions.assertEquals(null, perioder[0].fom)
        Assertions.assertEquals(sisteDagIJanuar, perioder[0].tom)
        Assertions.assertEquals("a", perioder[0].verdi)

        Assertions.assertEquals(førsteFebruar, perioder[1].fom)
        Assertions.assertEquals(sisteDagIFebruar, perioder[1].tom)
        Assertions.assertEquals("b", perioder[1].verdi)

        Assertions.assertEquals(førsteMars, perioder[2].fom)
        Assertions.assertEquals(null, perioder[2].tom)
        Assertions.assertEquals("c", perioder[2].verdi)
    }

    @Test
    fun `tilTidslinje - Skal kaste feil dersom det er flere tom-datoer med nullverdi`() {
        val perioder = listOf(
            Periode("a", null, sisteDagIJanuar),
            Periode("b", førsteFebruar, null),
            Periode("c", førsteMars, null)
        )

        Assertions.assertThrows(Exception::class.java) { perioder.tilTidslinje() }
    }

    @Test
    fun `tilTidslinje - Skal kaste feil dersom det er flere fom-datoer med nullverdi`() {
        val perioder = listOf(
            Periode("a", null, sisteDagIJanuar),
            Periode("b", null, sisteDagIFebruar),
            Periode("c", førsteMars, null)
        )

        Assertions.assertThrows(Exception::class.java) { perioder.tilTidslinje() }
    }

    @Test
    fun `tilTidslinje - Skal kaste feil om det er overlapp i periodene`() {
        val perioder = listOf(
            Periode("a", null, sisteDagIJanuar),
            Periode("b", førsteFebruar, sisteDagIMars),
            Periode("c", førsteMars, null)
        )

        Assertions.assertThrows(Exception::class.java) { perioder.tilTidslinje() }
    }

    @Test
    fun `tilTidslinje og tilPerioder - Skal håndtere tom liste`() {
        val perioder = emptyList<Periode<Any>>()

        Assertions.assertEquals(0, perioder.tilTidslinje().tilPerioder().size)
    }
}