package com.dagger.ble_background

data class GeofenceModel(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val transitionTypes: Int
)