package org.example.util

import com.google.gson.Gson
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Any.toJsonString(): String = Gson().toJson(this)

val Any.log: Logger
    get() = LoggerFactory.getLogger(this.javaClass)

val json = Json { ignoreUnknownKeys = true }