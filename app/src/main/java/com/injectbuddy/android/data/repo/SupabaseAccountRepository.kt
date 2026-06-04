package com.injectbuddy.android.data.repo

import com.injectbuddy.android.data.model.SavedDosage
import com.injectbuddy.android.data.model.UserProfile
import com.injectbuddy.android.domain.AccountRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Supabase-backed profile + saved-dosage access. RLS scopes every row to the signed-in
 * user, so queries here intentionally carry no user_id filter on SELECT — except writes,
 * where we stamp user_id explicitly (the column is NOT NULL and RLS checks it). Mirrors
 * the web's lib/account.ts loader shape + /api/dosages.
 */
class SupabaseAccountRepository(
    private val supabase: SupabaseClient,
) : AccountRepository {

    /** profiles holds only display_name; email lives on the auth user, not the table. */
    override suspend fun getProfile(): Result<UserProfile> = runCatching {
        val user = supabase.auth.currentUserOrNull()
            ?: error("Not signed in")

        // profiles row may not exist yet for a fresh account — tolerate its absence and
        // fall back to the auth user's email-derived name (parity with lib/account.ts).
        val row = supabase.from("profiles")
            .select(Columns.list("id", "display_name")) {
                filter { eq("id", user.id) }
            }
            .decodeSingleOrNull<ProfileRow>()

        UserProfile(
            id = user.id,
            displayName = row?.displayName ?: user.email?.substringBefore('@'),
            email = user.email,
            avatarUrl = user.userMetadata?.get("avatar_url")?.let { (it as? JsonPrimitive)?.content },
        )
    }

    override suspend fun listSavedDosages(calculatorType: String?): Result<List<SavedDosage>> =
        runCatching {
            supabase.from("saved_dosages")
                .select(Columns.ALL) {
                    filter { if (calculatorType != null) eq("calculator_type", calculatorType) }
                    order("created_at", Order.DESCENDING)
                    limit(50)
                }
                .decodeList<SavedDosage>()
        }

    override suspend fun saveDosage(
        calculatorType: String,
        label: String?,
        config: JsonObject,
    ): Result<String> = runCatching {
        val user = supabase.auth.currentUserOrNull()
            ?: error("Not signed in")

        // Build the insert payload as JSON so `config` (already a JsonObject) nests
        // verbatim — no intermediate DTO that would need to re-serialize the free-form map.
        val payload = buildJsonObject {
            put("user_id", JsonPrimitive(user.id))
            put("calculator_type", JsonPrimitive(calculatorType))
            if (label != null) put("label", JsonPrimitive(label))
            put("config", config)
        }

        supabase.from("saved_dosages")
            .insert(payload) { select(Columns.list("id")) }
            .decodeSingle<IdRow>()
            .id
    }

    override suspend fun setDosageActive(id: String, active: Boolean): Result<Unit> = runCatching {
        supabase.from("saved_dosages")
            .update({ set("is_active", active) }) {
                filter { eq("id", id) }
            }
    }

    override suspend fun deleteDosage(id: String): Result<Unit> = runCatching {
        supabase.from("saved_dosages")
            .delete { filter { eq("id", id) } }
    }
}

/** Minimal decode targets — kept private; the public surface is the domain models. */
@kotlinx.serialization.Serializable
private data class ProfileRow(
    @kotlinx.serialization.SerialName("display_name") val displayName: String? = null,
)

@kotlinx.serialization.Serializable
private data class IdRow(val id: String)
