package com.benkolera.upbank.util

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*

object Ktor {
    fun client(engine: HttpClientEngine) = HttpClient(engine) {
        install(JsonFeature) {
            serializer = JacksonSerializer(Jackson.mapper())
        }
    }

    fun prodClient() = client(CIO.create {
        //CIO Specific config goes here
    })
}
