package com.example.binraiders

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object ApiClient {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun getEventos(): List<Evento> = withContext(Dispatchers.IO) {
        val url = ApiConfig.BASE_URL.trimEnd('/') + "/eventos"
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@withContext emptyList()
            val body = res.body?.string().orEmpty()
            parseEventos(body)
        }
    }

    suspend fun postEventoDemo(msg: String): Evento? = withContext(Dispatchers.IO) {
        val url = ApiConfig.BASE_URL.trimEnd('/') + "/eventos"
        val json = JSONObject().apply {
            put("contenedor_id", 1)
            put("nivel", "INFO")
            put("mensaje", msg)
            put("distancia_cm", JSONObject.NULL)
        }.toString()

        val req = Request.Builder()
            .url(url)
            .post(json.toRequestBody(JSON))
            .build()

        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@withContext null
            val body = res.body?.string().orEmpty()
            parseEvento(body)
        }
    }

    private fun parseEventos(raw: String): List<Evento> {
        val arr = JSONArray(raw)
        val out = ArrayList<Evento>()
        for (i in 0 until arr.length()) {
            out.add(parseEvento(arr.getJSONObject(i).toString())!!)
        }
        return out
    }

    private fun parseEvento(raw: String): Evento? {
        return try {
            val o = JSONObject(raw)
            Evento(
                id = o.getInt("id"),
                ts = o.getString("ts"),
                contenedor_id = o.getInt("contenedor_id"),
                nivel = o.getString("nivel"),
                mensaje = o.getString("mensaje"),
                distancia_cm = if (o.isNull("distancia_cm")) null else o.getInt("distancia_cm"),
                acknowledged = o.optBoolean("acknowledged", false)
            )
        } catch (e: Exception) {
            null
        }
    }
}
