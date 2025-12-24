package com.example.binraiders

data class Evento(
    val id: Int,
    val ts: String,
    val contenedor_id: Int,
    val nivel: String,
    val mensaje: String,
    val distancia_cm: Int? = null,
    val acknowledged: Boolean = false
)
