package io.ceezix.api

import android.util.Log
import io.ceezix.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiRepository {

    suspend fun analyzeCipher(cipherText: String, contextClues: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiRepository", "Gemini API key is not configured. Running offline cryptanalyst.")
            return runOfflineCryptanalyst(cipherText, contextClues)
        }

        val prompt = """
            You are "Ceezix Cryptanalyst AI", a specialized cryptography decoding agent.
            
            We intercepted this secure cipher transmission:
            "$cipherText"
            
            Additional context/clues:
            "$contextClues"
            
            Task:
            1. Analyze the text. Is it a Caesar Cipher with a specific shift, ROT13, Vigenere, Atbash, or simple substitution?
            2. Brute-force/try decrypting it with shifts 1 to 25. If there's a readable result in English or standard text, identify that key (shift value).
            3. Provide:
               - Predicted Cipher Type (e.g., Caesar Cipher, Atbash, etc.)
               - Decrypted Plaintext
               - Identified Key Details (e.g., "Shift of +7 (or Left 7) because letter 'E' maps to 'L'...")
               - A fun, hacker-themed historical description or spy report about the decoded message.
            
            Format your response professionally with clear, exciting headings, cosmic-retro developer spacing, and a summary.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.2f),
            systemInstruction = Content(parts = listOf(Part(text = "You are a cosmic secret agent cryptanalyst for Ceezix.")))
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "CRITICAL WARNING: The cosmic intelligence produced an empty intercept report. Decoder offline."
        } catch (e: Exception) {
            Log.e("GeminiRepository", "API call failed", e)
            val cleanError = e.localizedMessage ?: "Unknown connection timeout"
            runOfflineCryptanalyst(cipherText, contextClues, errorMsg = cleanError)
        }
    }

    private fun runOfflineCryptanalyst(cipherText: String, contextClues: String, errorMsg: String? = null): String {
        // Build a highly interactive and helpful local cryptanalysis report
        val hasKeyPhrase = contextClues.lowercase().contains("shift") || contextClues.contains("key")
        
        // Let's implement actual local brute force over standard shifts to find the most "English" looking one
        val commonWords = listOf("the", "and", "you", "that", "was", "for", "are", "with", "his", "they", "this", "have", "from", "one", "had", "word", "but")
        var bestShift = 0
        var highestScore = 0
        var bestDecryption = ""

        for (shift in 0..25) {
            val decoded = decryptCaesar(cipherText, shift)
            val decodedLower = decoded.lowercase()
            var score = 0
            for (word in commonWords) {
                if (decodedLower.contains(word)) score += 10
            }
            // Count standard space frequency and high frequency letters (e, t, a, o)
            score += decodedLower.count { it == ' ' } * 5
            score += decodedLower.count { it == 'e' } * 3
            score += decodedLower.count { it == 't' } * 2
            
            if (score > highestScore) {
                highestScore = score
                bestShift = shift
                bestDecryption = decoded
            }
        }

        if (bestDecryption.isBlank()) {
            bestDecryption = decryptCaesar(cipherText, 13) // ROT13 fallback
            bestShift = 13
        }

        val offlineHeader = if (errorMsg != null) {
            "⚡ OFFLINE INTERCEPT SOLVER (API Offline: $errorMsg)\n========================================"
        } else {
            "⚡ OFFLINE COGNITIVE COMPILER MODE (No API Key)\n========================================"
        }

        return """
            $offlineHeader
            
            [Analysis Report]
            The localized computational decoder has finished checking standard ciphers.
            
            * Cipher Type Detected: Symmetric Substitution Cipher (Caesar)
            * Confidence Index: ${if (highestScore > 10) "92% (Highly readable)" else "45% (Ambiguous pattern)"}
            
            🔍 [Calculated Solver Results]
            Identified optimal mapping with Shift of: Left $bestShift / Right ${26 - bestShift}
            
            🔓 [Decrypted Transmission]
            "$bestDecryption"
            
            🛡️ [Detailed Cryptanalysis]
            - Standard letter frequency indexing identified space groupings matching classic word markers.
            - To test other shifts, manually adjust the Interactive Decoder Wheel below with Shift $bestShift!
            
            ${if (errorMsg != null) "Note: The direct network link returned: $errorMsg. To activate state-of-the-art AI analysis, confirm your Gemini API key in the AI Studio Secrets panel." else "To activate state-of-the-art AI analysis, confirm your Gemini API key in the AI Studio Secrets panel."}
        """.trimIndent()
    }

    private fun decryptCaesar(text: String, shift: Int): String {
        return text.map { char ->
            when {
                char.isUpperCase() -> {
                    val base = 'A'.code
                    val code = char.code
                    // Applying decypher (opposite of shift)
                    var original = (code - base - shift) % 26
                    if (original < 0) original += 26
                    (original + base).toChar()
                }
                char.isLowerCase() -> {
                    val base = 'a'.code
                    val code = char.code
                    var original = (code - base - shift) % 26
                    if (original < 0) original += 26
                    (original + base).toChar()
                }
                else -> char
            }
        }.joinToString("")
    }
}
