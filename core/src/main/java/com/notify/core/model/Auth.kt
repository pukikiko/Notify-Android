package com.notify.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class UserSettings(
    @SerialName("preferredFormat") val preferredFormat: String = "opus-160",
    @SerialName("displayName") val displayName: String? = null
)

@Serializable
data class User(
    @Serializable(with = FlexibleIdSerializer::class) val id: String,
    val username: String,
    val settings: UserSettings? = null
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: User
)

@Serializable
data class MeResponse(
    val user: User
)

@Serializable
data class SettingsResponse(
    val settings: UserSettings
)
