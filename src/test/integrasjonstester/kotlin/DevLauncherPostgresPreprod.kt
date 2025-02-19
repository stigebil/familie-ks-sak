package no.nav.familie.ks.sak

import no.nav.familie.ks.sak.config.ApplicationConfig
import no.nav.familie.ks.sak.config.DbContainerInitializer
import org.springframework.boot.builder.SpringApplicationBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

class DevLauncherPostgresPreprod

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "dev-postgres-preprod")
    val springBuilder =
        SpringApplicationBuilder(ApplicationConfig::class.java).profiles(
            "mock-økonomi",
            "mock-infotrygd-replika",
        )

    if (args.contains("--dbcontainer")) {
        springBuilder.initializers(DbContainerInitializer())
    }

    settClientIdOgSecret()

    springBuilder.run(*args)
}

private fun settClientIdOgSecret() {
    val cmd = "./hentMiljøvariabler.sh"

    val process = ProcessBuilder(cmd).start()

    val status = process.waitFor()
    if (status == 1) {
        error("Klarte ikke hente variabler fra Nais. Er du logget på Naisdevice og gcloud?")
    } else if (status == 2) {
        error("Feil context satt for kubectl, du må bruke dev-gcp?")
    }

    val inputStream = BufferedReader(InputStreamReader(process.inputStream))
    inputStream.readLine() // "Switched to context dev-gcp"
    val clientIdOgSecret = inputStream.readLine().split(";")
    inputStream.close()

    clientIdOgSecret.forEach {
        val keyValuePar = it.split("=")
        System.setProperty(keyValuePar[0], keyValuePar[1])
    }
}
