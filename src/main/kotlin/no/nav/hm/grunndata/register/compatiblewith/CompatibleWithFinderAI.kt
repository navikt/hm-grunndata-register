package no.nav.hm.grunndata.register.compatiblewith

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.api.GenerationConfig
import com.google.cloud.vertexai.api.Schema
import com.google.cloud.vertexai.api.Type
import com.google.cloud.vertexai.generativeai.ContentMaker
import com.google.cloud.vertexai.generativeai.GenerativeModel
import com.google.cloud.vertexai.generativeai.ResponseHandler
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.annotation.Introspected
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import kotlin.collections.joinToString
import kotlin.jvm.Throws
import kotlin.jvm.java
import kotlin.text.replace
import kotlin.text.trim
import kotlin.text.trimIndent
import kotlin.use

@Singleton
open class CompatibleAIFinder(private val config: VertexAIConfig, private val objectMapper: ObjectMapper ) {
    val instruction: String = """
        Du jobber i en NAV hjelpemiddelsentral, og bruker finnhjelpemiddel.no for informasjon.
    """.trimIndent()


    open fun findCompatibleProducts(partsTitle: String, mainTitles: List<HmsNrTitlePair>): List<HmsNr> {
        val prompt = generatePrompt(partsTitle, mainTitles)
        LOG.debug("Generated prompt: $prompt")
        return modelGenerateContent(prompt)
    }

    open fun findServiceableProducts(serviceTitle: String, mainTitles: List<HmsNrTitlePair>): List<HmsNr> {
        val prompt = generatePromptServiceFor(serviceTitle, mainTitles)
        LOG.debug("Generated prompt for serviceable products: $prompt")
        return modelGenerateContent(prompt)
    }

    open fun generatePrompt(accessory: String, mainProducts: List<HmsNrTitlePair>): String {
        val mainProductsString = mainProducts.joinToString(",") { "hmsnr=${it.hmsNr}: '${it.title.replace("'"," ")}'" }
        return "For følgende tilbehør: '${accessory.replace("'", " ").replace(",", " ")}' Finn ut hvilket hjelpemiddel som passer best blant disse: $mainProductsString \n svar med hmsnr"
            .trimIndent().trim()
    }

    open fun generatePromptServiceFor(service: String, mainProducts: List<HmsNrTitlePair>): String {
        val mainProductsString = mainProducts.joinToString(",") { "hmsnr=${it.hmsNr}: '${it.title.replace("'"," ")}'" }
        return "For følgende tjeneste: '${service.replace("'", " ").replace(",", " ")}' Finn ut hvilket hjelpemiddel som passer best blant disse: $mainProductsString \n svar med hmsnr"
            .trimIndent().trim()
    }

    @Throws(Exception::class)
    private fun modelGenerateContent(prompt: String): List<HmsNr> {
        VertexAI(config.project, config.location).use { vertexAI ->
            LOG.debug("Creating model for project ${config.project} in location ${config.location} with model ${config.model} and temperature ${config.temperature}")
            val generationConfig: GenerationConfig = GenerationConfig.newBuilder()
                .setResponseMimeType("application/json")
                .setTemperature(config.temperature)
                .setResponseSchema(
                    Schema.newBuilder()
                        .setType(Type.ARRAY)
                        .setItems(
                            Schema.newBuilder()
                                .setType(Type.OBJECT)
                                .putProperties("hmsnr", Schema.newBuilder().setType(Type.STRING).build())
                                .addAllRequired(listOf("hmsnr"))
                                .build()
                        )
                        .build()
                )
                .build()
            val model = GenerativeModel(config.model, vertexAI)
                .withGenerationConfig(generationConfig)
                .withSystemInstruction(ContentMaker.fromString(instruction))
            val response = model.generateContent(prompt)
            val output: String = ResponseHandler.getText(response)
            LOG.debug("${config.project} ${config.location} - Generating content for model ${config.model} with temp: ${config.temperature} with prompt: $prompt")
            LOG.debug("Got response: $output")
            return objectMapper.readValue(output,object : com.fasterxml.jackson.core.type.TypeReference<List<HmsNr>>() {} )
        }
    }
    companion object {
        private val LOG = LoggerFactory.getLogger(CompatibleAIFinder::class.java)
    }
}

@ConfigurationProperties("vertexai")
open class VertexAIConfig {
    var model: String = "gemini-2.0-flash-001"
    var location: String = "europe-north1"
    var project: String = "teamdigihot-dev-9705"
    var temperature: Float = 0.1f
}

@Introspected
data class HmsNr(
    val hmsnr: String
)

@Introspected
data class HmsNrTitlePair(val hmsNr: String, val title: String)

@Introspected
data class CompatibleProductResult(
    val score: Double = 0.0,
    val title: String,
    val seriesTitle: String,
    val seriesId: String,
    val productId: String,
    val hmsArtNr: String,
)

@Introspected
data class ServiceForResult(
    val title: String,
    val seriesId: String,
    val productId: String,
    val hmsArtNr: String,
)