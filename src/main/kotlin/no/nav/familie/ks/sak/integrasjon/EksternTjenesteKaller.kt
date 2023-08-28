package no.nav.familie.ks.sak.integrasjon

import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.ks.sak.common.exception.IntegrasjonException
import org.slf4j.LoggerFactory
import org.springframework.web.client.HttpClientErrorException
import java.net.URI

val logger = LoggerFactory.getLogger("eksternTjenesteKaller")
val secureLogger = LoggerFactory.getLogger("secureLogger")
inline fun <reified Data> kallEksternTjeneste(
    tjeneste: String,
    uri: URI,
    formål: String,
    eksterntKall: () -> Data,
): Data {
    loggEksternKall(tjeneste, uri, formål)

    return try {
        eksterntKall().also {
            logger.info("${lagEksternKallPreMelding(tjeneste, uri)} Kall ok")
        }
    } catch (exception: Exception) {
        throw handleException(exception = exception, tjeneste = tjeneste, uri = uri, formål = formål)
    }
}

inline fun <reified Data> kallEksternTjenesteRessurs(
    tjeneste: String,
    uri: URI,
    formål: String,
    eksterntKall: () -> Ressurs<Data>,
): Data {
    loggEksternKall(tjeneste, uri, formål)

    return try {
        eksterntKall().getDataOrThrow().also {
            logger.info("${lagEksternKallPreMelding(tjeneste, uri)} Kall ok")
        }
    } catch (exception: Exception) {
        throw handleException(exception = exception, tjeneste = tjeneste, uri = uri, formål = formål)
    }
}

inline fun <reified Data> kallEksternTjenesteUtenRespons(
    tjeneste: String,
    uri: URI,
    formål: String,
    eksterntKall: () -> Ressurs<Data>,
) {
    loggEksternKall(tjeneste, uri, formål)

    try {
        eksterntKall().also {
            logger.info("${lagEksternKallPreMelding(tjeneste, uri)} Kall ok")
        }
    } catch (exception: Exception) {
        throw handleException(exception = exception, tjeneste = tjeneste, uri = uri, formål = formål)
    }
}

fun lagEksternKallPreMelding(
    tjeneste: String,
    uri: URI,
) = "[tjeneste=$tjeneste, uri=$uri]"

fun loggEksternKall(
    tjeneste: String,
    uri: URI,
    formål: String,
) {
    logger.info("${lagEksternKallPreMelding(tjeneste, uri)} $formål")
}

fun handleException(
    exception: Exception,
    tjeneste: String,
    uri: URI,
    formål: String,
): Exception {
    return when (exception) {
        is RessursException -> {
            secureLogger.info(
                "${
                    lagEksternKallPreMelding(
                        tjeneste,
                        uri,
                    )
                } Kall mot $tjeneste feilet. Formål: $formål. Feilmelding: ${exception.ressurs.melding}",
                exception.cause,
            )
            logger.warn(
                "${
                    lagEksternKallPreMelding(
                        tjeneste,
                        uri,
                    )
                } Kall mot $tjeneste feilet. Formål: $formål.",
            )
            exception
        }
        is HttpClientErrorException -> exception
        else -> opprettIntegrasjonsException(tjeneste, uri, exception, formål)
    }
}

private fun opprettIntegrasjonsException(
    tjeneste: String,
    uri: URI,
    exception: Exception,
    formål: String,
): IntegrasjonException {
    val melding = if (exception is RessursException) {
        exception.ressurs.melding
    } else {
        exception.message
    }
    return IntegrasjonException(
        msg = "${lagEksternKallPreMelding(tjeneste, uri)} Kall mot \"$tjeneste\" feilet. Formål: $formål. Feilmelding: $melding",
        uri = uri,
        throwable = exception,
    )
}
