package com.takat.finanzas.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class IdResponse(val id: Long)

@Serializable
data class OkResponse(val ok: Boolean = true)
