package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.example.model.VideoAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class PadelChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedStroke: String? = null
)

object GeminiChatService {
    private const val TAG = "GeminiChatService"
    private const val PRIMARY_MODEL = "gemini-2.5-flash"
    private const val FALLBACK_MODEL = "gemini-3.5-flash"

    private const val SYSTEM_INSTRUCTION = """
Eres el Coach Principal de Pádel y Especialista en Biomecánica y Análisis de Vídeo en PadelSync.
Tu misión es asesorar a jugadores de todos los niveles (Iniciación, Intermedio, Avanzado y Competición) sobre técnica biomecánica de golpes (bandeja, víbora, remate x3, bajada de pared, voleas de derecha y revés, salida de pared y globo defensivo).
Analiza las dudas del jugador teniendo en cuenta la biomecánica correcta:
1. Posición de espera y lectura temprana de la bola rival.
2. Juego de pies y distancia al cristal o reja (pasos de ajuste, flexión de rodillas y transferencia de peso).
3. Punto de impacto (siempre adelantado y a la altura óptima del golpe).
4. Terminación de la pala y efecto deseado (cortado/slice, liftado/topspin o plano).
5. Táctica y selección de zonas de la pista (malla metálica, rincón, pies de los rivales, pasillo central).

Ofrece consejos prácticos y drills concretos de entrenamiento. Sé siempre motivador, didáctico y directo. Responde en español y utiliza emojis de pádel y deporte para que la lectura sea atractiva y fácil de aplicar en la pista.
"""

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        history: List<PadelChatMessage>,
        userMessage: String,
        currentAnalysis: VideoAnalysis? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            // Try primary model gemini-2.5-flash
            val primaryResponse = callGeminiGenerate(PRIMARY_MODEL, apiKey, history, userMessage, currentAnalysis)
            if (!primaryResponse.isNullOrBlank()) {
                return@withContext primaryResponse
            }

            // Fallback to gemini-3.5-flash
            val fallbackResponse = callGeminiGenerate(FALLBACK_MODEL, apiKey, history, userMessage, currentAnalysis)
            if (!fallbackResponse.isNullOrBlank()) {
                return@withContext fallbackResponse
            }
        }

        // Smart offline/expert response engine
        generateCoachFallbackResponse(userMessage, currentAnalysis)
    }

    private fun callGeminiGenerate(
        model: String,
        apiKey: String,
        history: List<PadelChatMessage>,
        userMessage: String,
        currentAnalysis: VideoAnalysis?
    ): String? {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            // Build system instruction
            var systemText = SYSTEM_INSTRUCTION
            if (currentAnalysis != null) {
                systemText += "\n\n[CONTEXTO DEL ANÁLISIS DE VÍDEO ACTUAL DEL JUGADOR]:\n" +
                    "- Golpe analizado: ${currentAnalysis.strokeType}\n" +
                    "- Puntuación técnica: ${currentAnalysis.score}/100 (${currentAnalysis.levelTier})\n" +
                    "- Puntos fuertes observados: ${currentAnalysis.positiveFeedback}\n" +
                    "- Correcciones recomendadas: ${currentAnalysis.improvementFeedback}\n" +
                    "- Puntuaciones desglosadas: Pies ${currentAnalysis.footworkScore}/100, Impacto ${currentAnalysis.impactScore}/100, Terminación ${currentAnalysis.followThroughScore}/100, Táctica ${currentAnalysis.tacticsScore}/100.\n" +
                    "Utiliza estos datos cuando el usuario pregunte sobre su golpe o vídeo actual."
            }

            val systemParts = JSONArray().put(JSONObject().put("text", systemText))
            val systemInstructionObj = JSONObject().put("parts", systemParts)

            // Build multi-turn contents
            val contentsArray = JSONArray()

            // Add previous history turns
            history.takeLast(10).forEach { msg ->
                val role = if (msg.role == "user") "user" else "model"
                val parts = JSONArray().put(JSONObject().put("text", msg.text))
                contentsArray.put(JSONObject().put("role", role).put("parts", parts))
            }

            // Add current user message
            val currentParts = JSONArray().put(JSONObject().put("text", userMessage))
            contentsArray.put(JSONObject().put("role", "user").put("parts", currentParts))

            val generationConfig = JSONObject()
                .put("temperature", 0.7)
                .put("maxOutputTokens", 1200)

            val rootRequest = JSONObject()
                .put("systemInstruction", systemInstructionObj)
                .put("contents", contentsArray)
                .put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootRequest.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Model $model call returned HTTP ${response.code}: ${response.body?.string()}")
                return null
            }

            val body = response.body?.string() ?: return null
            val jsonRoot = JSONObject(body)
            val candidates = jsonRoot.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            parts.getJSONObject(0).optString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini Chat with model $model", e)
            null
        }
    }

    private fun generateCoachFallbackResponse(
        userMessage: String,
        currentAnalysis: VideoAnalysis?
    ): String {
        val lower = userMessage.lowercase()

        return when {
            lower.contains("bandeja") -> {
                """
🎾 **Claves Maestras de la Bandeja (Coach Gemini)**:
1. **Posición de Trofeo Inmediata**: En cuanto leas el globo del rival, perfila el cuerpo de costado y levanta ambos brazos. La pala debe estar a la altura de la cabeza sin llevarla atrás como en tenis.
2. **Punto de Impacto Adelantado**: No dejes caer la pelota detrás de la cabeza. Golpea 20 cm por delante de la coronilla y a la altura de los ojos.
3. **Efecto Cortado (Slice)**: Entra por debajo de la bola acariciando con la empuñadura continental para que al rebotar en la reja o cristal rival no se levante.
4. **Terminación**: Acaba apuntando con la pala hacia el bolsillo contrario.

🎯 **Drill para la pista**: Haz series de 15 bandejas desde mitad de pista buscando botar la bola antes de la línea de saque para que muera en la malla metálica.
""".trimIndent()
            }

            lower.contains("víbora") || lower.contains("vibora") -> {
                """
⚡ **Perfeccionando la Víbora Ofensiva**:
1. **Impacto Lateral a la Altura de la Sien**: A diferencia de la bandeja que es alta, la víbora se golpea más baja y abierta, a la altura de las 2 en el reloj (diestros).
2. **Cepillado de Bola**: Rodea la pelota por su hemisferio derecho dándole un efecto de rotación lateral endiablado.
3. **Pies Rápidos**: Tras impactar, da dos pasos rápidos hacia la red para cubrir el contragolpe rival en la 'T'.
4. **Objetivo Táctico**: Busca siempre la esquina del revés rival para que la bola toque cristal lateral y resbale por el fondo.

🎯 **Drill recomendado**: 3 series de 10 víboras buscando que la bola dibuje una trayectoria rasante tocando pared lateral antes que fondo.
""".trimIndent()
            }

            lower.contains("remate") || lower.contains("x3") || lower.contains("por 3") || lower.contains("smash") -> {
                """
🚀 **Técnica del Remate por 3 Metros (x3)**:
1. **Posición Bajo la Pelota**: Colócate justo debajo o ligeramente por detrás de la bola, arqueando la espalda para usar el core y no solo el brazo.
2. **Punto Más Alto con Efecto Topspin**: Golpea a las 12 en punto peinando la bola de abajo hacia arriba para darle aceleración vertical y rotación liftada.
3. **Pronación de Muñeca**: Rompe la muñeca hacia afuera en el milisegundo del contacto para abrir el ángulo hacia el cristal de fondo.
4. **Bote Ideal**: La bola debe botar a 1-1.5 metros del cristal de fondo para salir despedida por encima de la valla lateral de 3 metros.
""".trimIndent()
            }

            lower.contains("volea") -> {
                """
🛡️ **Volea de Control y Bloqueo en Red**:
1. **Pala Siempre Alta**: Mantén la pala a la altura del pecho en posición de espera. Si la pala baja, llegas tarde.
2. **Paso Adelante al Impactar**: Da un paso firme con la pierna opuesta para transmitir el peso del cuerpo a la bola sin balancear el brazo hacia atrás.
3. **Muñeca Rígida**: No des 'muñecazos'. La pala actúa como un muro firme que redirige la velocidad del rival con profundidad hacia las esquinas.
""".trimIndent()
            }

            lower.contains("pared") || lower.contains("bajada") || lower.contains("salida") -> {
                """
🧱 **Dominio de Paredes y Salidas de Pared**:
1. **Paciencia y Espacio**: Deja pasar la bola y mantén al menos 80 cm de distancia con el cristal para tener recorrido de golpeo.
2. **Baja el Centro de Gravedad**: Flexiona las rodillas para levantarte junto con el bote de la bola.
3. **Bajada de Pared Ofensiva**: Si la bola queda alta tras la pared, entra con paso agresivo e impacta hacia abajo buscando los pies del rival en la red.
""".trimIndent()
            }

            currentAnalysis != null && (lower.contains("mi golpe") || lower.contains("este video") || lower.contains("análisis") || lower.contains("mejorar") || lower.contains("error")) -> {
                """
🎯 **Feedback Personalizado sobre tu ${currentAnalysis.strokeType}**:
- **Puntuación registrada**: ${currentAnalysis.score}/100 ⭐
- **Puntos clave observados**: ${currentAnalysis.positiveFeedback}
- **Correcciones inmediatas**: ${currentAnalysis.improvementFeedback}

💡 **Consejo del Coach**: Enfócate en tu juego de pies (${currentAnalysis.footworkScore}/100) y en adelantar el punto de impacto (${currentAnalysis.impactScore}/100). Realiza el drill recomendado:
"${currentAnalysis.recommendedDrill}"
¡Grábate de nuevo tras 2 entrenamientos para comprobar tu evolución en la gráfica!
""".trimIndent()
            }

            else -> {
                """
🎾 **¡Hola! Soy tu Coach Gemini IA de PadelSync**.
Estoy listo para ayudarte a llevar tu nivel de juego a lo más alto.

Puedes preguntarme sobre:
- **Técnica de golpeos**: Bandeja, víbora, remate x3, voleas o bajadas de pared.
- **Dudas sobre tus vídeos analizados**: Te explico cómo corregir los fallos de impacto o juego de pies.
- **Drills y ejercicios**: Rutinas específicas para entrenar en pista o en casa.
- **Táctica de partido**: Cómo jugar con viento, contra pegadores o cerrar puntos en la red.

¿En qué aspecto técnico o táctico quieres que nos enfoquemos hoy?
""".trimIndent()
            }
        }
    }
}
