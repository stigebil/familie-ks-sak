package no.nav.familie.ks.sak.common.tidslinje

import no.nav.familie.ks.sak.common.tidslinje.utvidelser.mapper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class TidsEnhet {
    DAG,
    UKE,
    MÅNED,
    ÅR
}

/**
 * En tidslinje består av ulike verdier over tid. Det vil si at en tidslinje kan ha en verdi
 * A over et tidsintervall og en annen verdi B over et senere tidsintervall. Disse tidsintervallene blir håndert av Periode-klassen.
 * En tidslinje kan visualiseres som en liste [A, A, A, A, ... , A, B, B, ... , B, C, C, ... , C] og et startstidspunkt,
 * hvor A, B og C er ulike verdier og hver plass i lista representerer en dag.
 * Tidslinjer kan innholde uendelige perioder. Det kan ikke være flere perioder med andre verdier etter en uendelig periode.
 * En tidslinje støtter verdier av typen [Udefinert], [Null] og [PeriodeVerdi]. En verdi er udefinert når vi ikke vet
 * hva verdien skal være (et hull i tidslinja). En verdi er no.nav.familie.ks.sak.common.tidslinje.Null når vi vet at det ikke finnes noe verdi i dette tidsrommet.
 */
open class Tidslinje<T>(
    var startsTidspunkt: LocalDate,
    perioder: List<TidslinjePeriode<T>>,
    var tidsEnhet: TidsEnhet = TidsEnhet.DAG
) {

    var innhold: List<TidslinjePeriode<T>> = emptyList()
        set(verdi) {
            field = this.lagInnholdBasertPåPeriodelengder(verdi)
        }
    val foreldre: MutableList<Tidslinje<Any>> = mutableListOf()
    var tittel = ""

    init {
        this.innhold = perioder
        startsTidspunkt = when (tidsEnhet) {
            TidsEnhet.ÅR -> startsTidspunkt.withDayOfYear(1)
            TidsEnhet.MÅNED -> startsTidspunkt.withDayOfMonth(1)
            TidsEnhet.UKE -> startsTidspunkt.with(DayOfWeek.MONDAY)
            TidsEnhet.DAG -> startsTidspunkt
        }
    }

    /**
     * Kalkulerer slutttidspunkt som en LocalDate.
     * Funkjsonen returnerer den siste dagen som er med i tidslinja
     * Om tidslinja er uendelig, kastes det et unntak
     */
    fun kalkulerSluttTidspunkt(): LocalDate {
        val antallTidsEnheter: Int = this.innhold.sumOf { it.lengde }
        val sluttTidspunkt = this.startsTidspunkt.plus(antallTidsEnheter.toLong() - 1, mapper[this.tidsEnhet])

        return when (this.tidsEnhet) {
            TidsEnhet.ÅR -> sluttTidspunkt.with(TemporalAdjusters.lastDayOfYear())
            TidsEnhet.MÅNED -> sluttTidspunkt.with(TemporalAdjusters.lastDayOfMonth())
            TidsEnhet.UKE -> sluttTidspunkt.with(DayOfWeek.SUNDAY)
            TidsEnhet.DAG -> sluttTidspunkt
        }
    }

    private fun kalkulerSluttTidspunkt(sluttDato: LocalDate): LocalDate {
        return when (this.tidsEnhet) {
            TidsEnhet.ÅR -> sluttDato.with(TemporalAdjusters.lastDayOfYear())
            TidsEnhet.MÅNED -> sluttDato.with(TemporalAdjusters.lastDayOfMonth())
            TidsEnhet.UKE -> sluttDato.with(DayOfWeek.SUNDAY)
            TidsEnhet.DAG -> sluttDato
        }
    }

    override fun toString(): String {
        return "StartTidspunkt: " + startsTidspunkt + " Tidsenhet: " + tidsEnhet +
            " Total lengde: " + innhold.sumOf { it.lengde } +
            " Perioder: " + innhold.mapIndexed { indeks, it ->
            "(Verdi: " + it.periodeVerdi.verdi.toString() +
                ", fom: " + startsTidspunkt.plus(innhold.take(indeks).sumOf { it.lengde }.toLong(), mapper[this.tidsEnhet]) +
                ", tom:" + kalkulerSluttTidspunkt(
                startsTidspunkt.plus(
                    innhold.take(indeks).sumOf { it.lengde }.toLong() + it.lengde - 1,
                    mapper[this.tidsEnhet]
                )
            ) + ")"
        }
    }

    private fun lagInnholdBasertPåPeriodelengder(innhold: List<TidslinjePeriode<T>>): List<TidslinjePeriode<T>> {
        val arr = mutableListOf<TidslinjePeriode<T>>()

        var i = 0
        while (i < innhold.size) {
            var j = i + 1
            var lengde = innhold[i].lengde

            while (j < innhold.size && innhold[i].periodeVerdi == innhold[j].periodeVerdi) {
                lengde += innhold[j].lengde
                j++
                if (j >= innhold.size) break
            }

            val tidslinjePeriode = TidslinjePeriode(
                periodeVerdi = innhold[i].periodeVerdi,
                lengde = lengde,
                erUendelig = false
            )
            i = j
            arr.add(tidslinjePeriode)
            if (tidslinjePeriode.erUendelig) return arr.toList()
        }

        return arr.toList()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Tidslinje<*>) return false
        return other.startsTidspunkt == this.startsTidspunkt &&
            other.innhold == this.innhold &&
            other.tidsEnhet == this.tidsEnhet &&
            other.tittel == this.tittel
    }

    override fun hashCode(): Int {
        return this.startsTidspunkt.hashCode() + this.innhold.hashCode() + this.tidsEnhet.hashCode() + this.tittel.hashCode()
    }

    companion object
}