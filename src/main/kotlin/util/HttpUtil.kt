package org.example.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object HttpUtil {
    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return call(request)
    }

    fun post(url: String, json: String): String {
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(mediaType))
            .build()

        return call(request)
    }

    private fun call(request: Request): String {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("Unexpected code $response")
            return response.body?.string() ?: ""
        }
    }
}