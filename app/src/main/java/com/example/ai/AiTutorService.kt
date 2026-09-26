package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AiTutorService {

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

  suspend fun askTutor(userQuestion: String, currentTopic: String): String = withContext(Dispatchers.IO) {
    val apiKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Exception) {
      ""
    }

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      return@withContext getOfflineFallbackResponse(userQuestion, currentTopic)
    }

    try {
      val systemPrompt = "You are Spark, a friendly and encouraging AI English Tutor. " +
          "The student is currently viewing a lesson slide about: '$currentTopic'. " +
          "Provide a clear, engaging, concise answer (under 120 words) with helpful examples or pronunciation tips."

      val systemInstructionPart = JSONObject().put("text", systemPrompt)
      val systemInstructionContent = JSONObject().put("parts", JSONArray().put(systemInstructionPart))

      val userPart = JSONObject().put("text", userQuestion)
      val userContent = JSONObject().put("parts", JSONArray().put(userPart))

      val payload = JSONObject().apply {
        put("contents", JSONArray().put(userContent))
        put("systemInstruction", systemInstructionContent)
      }

      val mediaType = "application/json".toMediaType()
      val body = payload.toString().toRequestBody(mediaType)

      val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

      val httpRequest = Request.Builder()
        .url(url)
        .post(body)
        .build()

      val response = client.newCall(httpRequest).execute()
      val responseBodyString = response.body?.string()

      if (response.isSuccessful && responseBodyString != null) {
        val rootObj = JSONObject(responseBodyString)
        val candidates = rootObj.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
          val firstCandidate = candidates.getJSONObject(0)
          val contentObj = firstCandidate.optJSONObject("content")
          val partsArray = contentObj?.optJSONArray("parts")
          if (partsArray != null && partsArray.length() > 0) {
            val text = partsArray.getJSONObject(0).optString("text", "")
            if (text.isNotBlank()) {
              return@withContext text.trim()
            }
          }
        }
      }
      return@withContext getOfflineFallbackResponse(userQuestion, currentTopic)
    } catch (e: Exception) {
      return@withContext getOfflineFallbackResponse(userQuestion, currentTopic)
    }
  }

  private fun getOfflineFallbackResponse(question: String, topic: String): String {
    val q = question.lowercase()
    return when {
      q.contains("hello") || q.contains("hi") || q.contains("hey") ->
        "Hello there! I'm Spark, your AI English Tutor. We are currently exploring '$topic'. What would you like to learn or practice?"
      q.contains("origin") || q.contains("viking") || q.contains("french") ->
        "In 1066, Norman French invaded Britain. Everyday farm words came from Old English (cow, pig, sheep), while regal dining words came from French (beef, pork, mutton)!"
      q.contains("lingua franca") || q.contains("everyone") ->
        "English became a global Lingua Franca because 3 out of every 4 English speakers today are non-native speakers! It's the standard for international coding, gaming, and aviation."
      q.contains("quiz") || q.contains("letter") ->
        "Did you know? The letter 'E' makes up over 11% of all English text, making it the most common letter in the dictionary!"
      else ->
        "Great question about '$topic'! English is full of fascinating history, rich vocabulary, and subtle grammar rules. Try asking about word origins, pronunciation tips, or grammar examples!"
    }
  }
}
