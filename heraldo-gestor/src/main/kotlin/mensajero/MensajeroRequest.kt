package com.netbug94.mensajero

import kotlinx.serialization.Serializable

@Serializable
data class MensajeroRequest(
    val phone: String,
    val message: String
)
