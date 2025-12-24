package com.example.binraiders

object ApiConfig {
    const val RASPBERRY_IP = "10.54.130.83"

    const val BASE_URL = "http://$RASPBERRY_IP:8000"

    const val ESTADO_URL = "$BASE_URL/estado"
    const val SNAPSHOT_URL = "$BASE_URL/snapshot"
    const val STREAM_URL = "$BASE_URL/stream"
}
