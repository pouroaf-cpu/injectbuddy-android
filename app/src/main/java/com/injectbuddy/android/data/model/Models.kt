package com.injectbuddy.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Data models mirroring the InjectBuddy Supabase schema (see Injectbuddy/lib/supabase/
 * types.ts). @SerialName keeps Kotlin camelCase while Postgrest reads/writes snake_case.
 * `config`/`result` are free-form JSON, kept as [JsonObject].
 */

@Serializable
data class UserProfile(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class SavedDosage(
    val id: String,
    @SerialName("calculator_type") val calculatorType: String,
    val label: String? = null,
    val config: JsonObject = JsonObject(emptyMap()),
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class Protocol(
    val id: String,
    val calculator: String,
    val name: String,
    val config: JsonObject = JsonObject(emptyMap()),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class Cycle(
    val id: String,
    val name: String,
    val goal: String? = null,
    val kind: String = "blast",
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("on_weeks") val onWeeks: Int? = null,
    val notes: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    val items: List<CycleItem> = emptyList(),
)

@Serializable
data class CycleItem(
    val id: String,
    @SerialName("cycle_id") val cycleId: String,
    @SerialName("protocol_id") val protocolId: String? = null,
    val label: String? = null,
    val compound: String? = null,
    val role: String = "primary",
    val phase: String = "on",
    val dose: Double? = null,
    @SerialName("dose_unit") val doseUnit: String? = null,
    val frequency: String? = null,
    @SerialName("half_life_days") val halfLifeDays: Double? = null,
    @SerialName("start_week") val startWeek: Int = 0,
    @SerialName("duration_weeks") val durationWeeks: Int? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
)
