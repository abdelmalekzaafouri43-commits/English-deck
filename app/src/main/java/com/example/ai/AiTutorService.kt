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
      val systemPrompt = "You are Spark, an expert English Grammar Coach & Pedagogical Assistant for English teachers and learners. " +
          "The current grammar module topic is: '$currentTopic'. " +
          "Your role is to explain English grammar rules clearly, break down sentence structure, provide formulas (e.g. Subject + Verb + Object), " +
          "correct learner mistakes line-by-line, suggest CEFR level adaptations (A1-C1), and offer practical teaching tips for ESL/EFL classrooms. " +
          "Keep answers concise, structured, encouraging, and clear (under 140 words)."

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
        "Hello! I'm Spark, your English Grammar & Pedagogy Coach 🤖. We are currently exploring '$topic'. Ask me to explain a rule, check a sentence, or give teaching tips!"
      q.contains("tense") || q.contains("present") || q.contains("past") || q.contains("perfect") ->
        "Great tense question! Present Perfect (Subject + have/has + V3) connects past actions to the present (e.g. 'I have lived here since 2020'). Contrast with Past Simple for finished times!"
      q.contains("conditional") || q.contains("if") ->
        "Conditionals rule breakdown:\n• 1st (Real Future): If + Present, Will + Verb.\n• 2nd (Unreal Present): If + Past, Would + Verb.\n• 3rd (Past Regret): If + Past Perfect, Would have + V3."
      q.contains("passive") || q.contains("active") ->
        "Active voice emphasizes WHO performs the action ('She wrote the report'). Passive voice emphasizes WHAT was done ('The report was written'). Formula: Subject + Be + V3 (Past Participle)."
      q.contains("article") || q.contains("a ") || q.contains("an ") || q.contains("the ") ->
        "Articles quick guide:\n• 'A/An' for non-specific singular countable nouns ('a book', 'an apple').\n• 'The' for specific or unique nouns known to both speaker & listener ('the sun')."
      q.contains("mistake") || q.contains("error") || q.contains("correct") ->
        "Common learner error: 'He go to school yesterday' ❌. Correction: 'He WENT to school yesterday' ✅ (Past Simple requires irregular V2 form)."
      else ->
        "Excellent grammar query regarding '$topic'! English syntax relies on clear word order (Subject + Verb + Object). Feel free to ask me to analyze any sentence or create a classroom drill!"
    }
  }
}
