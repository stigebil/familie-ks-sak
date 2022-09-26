package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockkObject
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import org.hamcrest.CoreMatchers.`is` as Is

internal class IntegrasjonClientTest {

    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private lateinit var integrasjonClient: IntegrasjonClient
    private lateinit var wiremockServerItem: WireMockServer

    @BeforeEach
    fun initClass() {
        wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServerItem.start()
        integrasjonClient = IntegrasjonClient(URI.create(wiremockServerItem.baseUrl()), restOperations)
    }

    @Test
    fun `hentOppgaver skal returnere en liste av oppgaver basert på request og tema`() {
        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/oppgave/v4"))
                .willReturn(WireMock.okJson(readFile("hentOppgaverEnkelKONResponse.json")))
        )

        val oppgaver = integrasjonClient.hentOppgaver(FinnOppgaveRequest(Tema.KON))

        assertThat(oppgaver.antallTreffTotalt, Is(1))
        assertThat(oppgaver.oppgaver.size, Is(1))

        assertThat(oppgaver.oppgaver.single().tema, Is(Tema.KON))
    }

    @Test
    fun `hentBehandlendeEnhet skal hente enhet fra familie-integrasjon`() {
        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/arbeidsfordeling/enhet/KON"))
                .willReturn(WireMock.okJson(readFile("hentBehandlendeEnhetEnkelResponse.json")))
        )

        val behandlendeEnheter = integrasjonClient.hentBehandlendeEnheter("testident")

        assertThat(behandlendeEnheter.size, Is(2))
        assertThat(behandlendeEnheter.map { it.enhetId }, containsInAnyOrder("enhetId1", "enhetId2"))
        assertThat(behandlendeEnheter.map { it.enhetNavn }, containsInAnyOrder("enhetNavn1", "enhetNavn2"))
    }

    @Test
    fun `hentNavKontorEnhet skal hente enhet fra familie-integrasjon`() {
        wiremockServerItem.stubFor(
            WireMock.get(WireMock.urlEqualTo("/arbeidsfordeling/nav-kontor/200"))
                .willReturn(WireMock.okJson(readFile("hentEnhetEnkelResponse.json")))
        )

        val navKontorEnhet = integrasjonClient.hentNavKontorEnhet("200")

        assertThat(navKontorEnhet.enhetId, Is(200))
        assertThat(navKontorEnhet.enhetNr, Is("200"))
        assertThat(navKontorEnhet.navn, Is("Riktig navn"))
        assertThat(navKontorEnhet.status, Is("Riktig status"))
    }

    @Test
    fun `finnOppgaveMedId skal hente oppgave med spesifikt id fra familie-integrasjon`() {
        wiremockServerItem.stubFor(
            WireMock.get(WireMock.urlEqualTo("/oppgave/200"))
                .willReturn(WireMock.okJson(readFile("finnOppgaveMedIdEnkelResponse.json")))
        )

        val oppgave = integrasjonClient.finnOppgaveMedId(200)

        assertThat(oppgave.id, Is(200))
        assertThat(oppgave.tildeltEnhetsnr, Is("4812"))
        assertThat(oppgave.endretAvEnhetsnr, Is("4812"))
        assertThat(oppgave.journalpostId, Is("123456789"))
        assertThat(oppgave.tema, Is(Tema.KON))
    }

    @Test
    fun `sjekkTilgangTilPersoner skal returnere Tilgang med true hvis SB har tilgang til alle personidenter`() {
        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/tilgang/v2/personer"))
                .willReturn(WireMock.okJson(readFile("sjekkTilgangTilPersonerResponseMedTilgangTilAlle.json")))
        )

        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erSystemKontekst() } returns false

        val tilgangTilPersonIdent = integrasjonClient.sjekkTilgangTilPersoner(listOf("ident1", "ident2", "ident3"))

        assertThat(tilgangTilPersonIdent.harTilgang, Is(true))
        assertThat(tilgangTilPersonIdent.begrunnelse, Is("Har tilgang"))
    }

    @Test
    fun `sjekkTilgangTilPersoner skal returnere Tilgang med false hvis SB ikke har tilgang til alle personidenter`() {
        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/tilgang/v2/personer"))
                .willReturn(WireMock.okJson(readFile("sjekkTilgangTilPersonerResponseMedIkkeTilgangTilAlle.json")))
        )

        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erSystemKontekst() } returns false

        val tilgangTilPersonIdent = integrasjonClient.sjekkTilgangTilPersoner(listOf("ident1", "ident2", "ident3"))

        assertThat(tilgangTilPersonIdent.harTilgang, Is(false))
        assertThat(tilgangTilPersonIdent.begrunnelse, Is("Har ikke tilgang"))
    }

    @Test
    fun `fordelOppgave skal returnere fordelt oppgave ved OK fordelelse av oppgave`() {
        val saksbehandler = "testSB"

        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/oppgave/200/fordel?saksbehandler=$saksbehandler"))
                .willReturn(WireMock.okJson(readFile("fordelOppgaveEnkelResponse.json")))
        )

        val fordeltOppgave = integrasjonClient.fordelOppgave(200, saksbehandler)

        assertThat(fordeltOppgave.oppgaveId, Is(200))
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/familieintegrasjon/json/$filnavn").readText()
    }
}
